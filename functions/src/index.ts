import * as admin from "firebase-admin";
import { firestore } from "firebase-admin";
import { GeoPoint } from "firebase-admin/firestore";
import * as functions from "firebase-functions";

const EARTH_RADIUS_MILES = 3959;
const ONE_MILE_LATITUDE_DEGREES = 0.014492753623188;
const NEARBY_PIN_RADIUS_MILES = 0.5;
const PIN_FIND_RADIUS_MILES = 0.0095; // 50 ft

enum PinType {
  TEXT = "TEXT",
  IMAGE = "IMAGE",
}

type Pin = {
  caption: string;
  textContent?: string;
  type: PinType;
  location: GeoPoint;
  authorUID: string;
  timestamp: Date;
  finds: number;
};

admin.initializeApp();

export const getNearbyPins = functions.https.onCall(
  // Validate auth status and args
  async ({ latitude, longitude }, context) => {
    if (!context || !context.auth || !context.auth.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "getNearbyPins must be called while authenticated."
      );
    }
    if (!latitude || !longitude) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "getNearbyPins must be called with a latitude and longitude."
      );
    }

    const pins = await getPinsNearby(
      latitude,
      longitude,
      NEARBY_PIN_RADIUS_MILES
    );
    return Object.fromEntries(pins.map((pin) => [pin.id, pin.data().location]));
  }
);

export const calcPinCost = functions.https.onCall(
  async ({ latitude, longitude }, context) => {
    if (!context || !context.auth || !context.auth.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "calcPinCost must be called while authenticated."
      );
    }
    if (!latitude || !longitude) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "calcPinCost must be called with a latitude and longitude."
      );
    }
    return await calculateCost(latitude, longitude);
  }
);

// TODO: add anti-spoof check before dropping pin
export const dropPin = functions.https.onCall(
  async ({ textContent, caption, type, latitude, longitude }, context) => {
    // Validate auth status and args
    if (!context || !context.auth || !context.auth.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "dropPin must be called while authenticated."
      );
    }
    if (
      !type ||
      !latitude ||
      !longitude ||
      (type === PinType.TEXT && !textContent)
    ) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "dropPin must be called with proper arguments."
      );
    }

    const pin: Pin = {
      caption,
      type,
      location: new GeoPoint(latitude, longitude),
      authorUID: context.auth.uid,
      timestamp: new Date(),
      finds: 0,
    };

    if (type === PinType.TEXT) pin.textContent = textContent;

    // Get document references for reading/writing
    const privateDataRef = firestore()
      .collection("private")
      .doc(context.auth.uid);
    const pinRef = firestore().collection("pins").doc();
    const droppedRef = firestore()
      .collection("users")
      .doc(context.auth.uid)
      .collection("dropped")
      .doc(pinRef.id);

    // Perform validation & updates in transaction to enforce all or none
    await firestore().runTransaction(async (t) => {
      // Read required data & calculate pin cost
      const privateData = (await t.get(privateDataRef)).data();
      const cost = await calculateCost(latitude, longitude, t);

      // Check user has sufficient currency
      if (
        !privateData ||
        !privateData.currency ||
        privateData.currency < cost
      ) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Insufficent funds."
        );
      }

      // Deduct currency & create pin
      t.update(privateDataRef, { currency: privateData.currency - cost });
      t.create(pinRef, pin);
      t.create(droppedRef, { cost, timestamp: new Date() });
    });
    return pinRef.id;
  }
);

// TODO: add anti-spoof check before finding pin
export const findPin = functions.https.onCall(
  async ({ pid, latitude, longitude }, context) => {
    // Validate auth status and args
    if (!context || !context.auth || !context.auth.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "dropPin must be called while authenticated."
      );
    }
    if (!pid || !latitude || !longitude) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "dropPin must be called with proper arguments."
      );
    }

    // Get document references for reading/writing
    const privateDataRef = firestore()
      .collection("private")
      .doc(context.auth.uid);
    const pinRef = firestore().collection("pins").doc(pid);
    const foundRef = firestore()
      .collection("users")
      .doc(context.auth.uid)
      .collection("found")
      .doc(pid);

    // Check user has not found pin yet
    const foundData = (await foundRef.get()).data();
    if (foundData !== undefined) {
      throw new functions.https.HttpsError(
        "already-exists",
        "You have already found this pin."
      );
    }

    // TODO: add anti-spoof check
    // const privateData = (await t.get(privateDataRef)).data();

    // Perform pin read, reward calculation, & writes in one transaction
    return await firestore().runTransaction(async (t) => {
      // Check pin exists
      const pinData: Pin = <Pin>(await pinRef.get()).data();
      if (pinData === undefined) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "No pin exists with provided pin ID."
        );
      }

      // Check pin is not authored by user
      if (pinData.authorUID === context.auth?.uid) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Cannot find your own pin."
        );
      }

      // Check location is close enough
      const distanceMiles = calcDistanceMiles(
        latitude,
        longitude,
        pinData.location.latitude,
        pinData.location.longitude
      );
      if (distanceMiles > PIN_FIND_RADIUS_MILES) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Not close enough to find pin!"
        );
      }

      const reward = await calculateReward(pinData, t);
      t.set(
        privateDataRef,
        { currency: firestore.FieldValue.increment(reward) },
        { merge: true }
      );
      t.set(
        pinRef,
        { finds: firestore.FieldValue.increment(1) },
        { merge: true }
      );
      t.create(foundRef, { reward, timestamp: new Date() });
      return pinData;
    });
  }
);

/**
 * calculateReward will give the first NUM_BONUS people an exponentially decaying bonus
 * above BASE, while pin finders after NUM_BONUS will get a reward slightly under BASE
 * SCALING_REDUCTION exists so that the first pin finder doesn't get like 10x BASE
 * reward scaling function visualized:
 * |
 * |
 * #
 * |#
 * | #
 * |  #
 * |   #
 * |    #
 * |     #
 * |      #
 * |       #
 * |        #
 * |          #
 * |           ##
 * |             ##
 * |                ##
 * |                  ###
 * |                      ###
 * |                          ####
 * |                               #####
 * |                                     ########
 * |                                               ##########
 * |                                                            ################
 * |                                                                                  ################
 * ___________________________________________________________________________________________________
 */
async function calculateReward(pin: Pin, transaction?: firestore.Transaction) {
  const BASE = 100;
  const NUM_BONUS = 20;
  const SCALING_REDUCTION = 2;

  return (NUM_BONUS / (pin.finds + SCALING_REDUCTION)) * BASE;
}

/**
 * calculateCost finds the number of pins within three progressively larger radii
 * and linearly scales the cost of a new pin according to number of neighbors
 */
async function calculateCost(
  latitude: number,
  longitude: number,
  transaction?: firestore.Transaction
) {
  const BASE = 60;
  const CLOSE_RADIUS = 0.1;
  const MID_RADIUS = 0.5;
  const FAR_RADIUS = 2;
  const CLOSE_SCALE = 1.0;
  const MID_SCALE = 0.25;
  const FAR_SCALE = 0.1;

  const close = (await getPinsNearby(latitude, longitude, CLOSE_RADIUS)).length;
  const mid =
    (await getPinsNearby(latitude, longitude, MID_RADIUS)).length - close;
  const far =
    (await getPinsNearby(latitude, longitude, FAR_RADIUS)).length - mid - close;

  console.log(`close: ${close}, mid: ${mid}, far: ${far}`);

  const cost = Math.round(
    BASE +
      BASE * CLOSE_SCALE * close + // number of very close pins
      BASE * MID_SCALE * mid + // number of relatively close pins
      BASE * FAR_SCALE * far
  ); // number of further away pins
  console.log(`price: ${cost}`);

  return cost;
}

async function getPinsNearby(
  latitude: number,
  longitude: number,
  radiusMiles: number,
  transaction?: firestore.Transaction
) {
  const oneMileLongitudeDegrees = calcOneMileLongitudeDegrees(latitude);

  const lowerLat = latitude - ONE_MILE_LATITUDE_DEGREES * radiusMiles;
  const lowerLon = longitude - oneMileLongitudeDegrees * radiusMiles;

  const upperLat = latitude + ONE_MILE_LATITUDE_DEGREES * radiusMiles;
  const upperLon = longitude + oneMileLongitudeDegrees * radiusMiles;

  const lowerGeopoint = new GeoPoint(lowerLat, lowerLon);
  const upperGeopoint = new GeoPoint(upperLat, upperLon);

  const query = firestore()
    .collection("pins")
    .where("location", ">", lowerGeopoint)
    .where("location", "<", upperGeopoint);

  const snapshot = await (transaction ? transaction.get(query) : query.get());

  return snapshot.docs;
}

function calcDistanceMiles(
  lat1: number,
  long1: number,
  lat2: number,
  long2: number
) {
  const oneMileLongitudeDegrees = calcOneMileLongitudeDegrees(lat1);
  const latitudeDiffMiles = (lat1 - lat2) / ONE_MILE_LATITUDE_DEGREES;
  const longitudeDiffMiles = (long1 - long2) / oneMileLongitudeDegrees;
  return Math.sqrt(latitudeDiffMiles ** 2 + longitudeDiffMiles ** 2);
}

function calcOneMileLongitudeDegrees(latitude: number) {
  return 1 / ((Math.PI / 180) * EARTH_RADIUS_MILES * Math.cos(latitude));
}
