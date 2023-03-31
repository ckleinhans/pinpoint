import * as admin from "firebase-admin";
import { firestore } from "firebase-admin";
import { GeoPoint } from "firebase-admin/firestore";
import * as functions from "firebase-functions";

const EARTH_RADIUS_MILES = 3959;
const ONE_MILE_LATITUDE_DEGREES = 0.014492753623188;
const NUM_MILES_NEARBY = 2;

enum PinType {
  TEXT,
  IMAGE,
}

type Pin = {
  caption: string;
  textContent?: string;
  type: PinType;
  location: GeoPoint;
  authorUID: string;
  timestamp: Date;
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

    const pins = await getPinsNearby(latitude, longitude, NUM_MILES_NEARBY);
    return Object.fromEntries(pins.map((pin) => [pin.id, pin.data().location]));
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
      (type == PinType.TEXT && !textContent)
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
    };

    if (type == PinType.TEXT) pin.textContent = textContent;

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
      t.create(droppedRef, { cost });
    });
    return pinRef.id;
  }
);

async function calculateCost(
  latitude: number,
  longitude: number,
  transaction?: firestore.Transaction
) {
  // TODO: implement algorithm for calculating pin cost
  return 200;
}

async function getPinsNearby(
  latitude: number,
  longitude: number,
  radiusMiles: number,
  transaction?: firestore.Transaction
) {
  const oneMileLongitudeDegrees =
    1 / ((Math.PI / 180) * EARTH_RADIUS_MILES * Math.cos(latitude));

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
