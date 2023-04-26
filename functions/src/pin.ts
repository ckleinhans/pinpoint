import { firestore } from "firebase-admin";
import { FieldValue, GeoPoint } from "firebase-admin/firestore";
import * as functions from "firebase-functions";
import { distanceBetween, geohashForLocation, Geopoint } from "geofire-common";

import { calculateCost, calculateReward } from "./cost";
import {
  Activity,
  ActivityType,
  Pin,
  PinMetadata,
  PinSource,
  PinType,
} from "./types";

const PIN_FIND_RADIUS_KILOMETERS = 0.02; // 20 meters

export const dropPinHandler = async (
  {
    textContent,
    caption,
    type,
    latitude,
    longitude,
    nearbyLocationName,
    broadLocationName,
  },
  context
) => {
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

  // Get document references for reading/writing
  const pinRef = firestore().collection("pins").doc();
  const userRef = firestore().collection("users").doc(context.auth.uid);
  const activityRef = userRef.collection("metadata").doc("activity");
  const privateDataRef = userRef.collection("metadata").doc("private");
  const droppedRef = userRef.collection("dropped").doc("dropped");

  // Perform validation & updates in transaction to enforce all or none
  await firestore().runTransaction(async (t) => {
    const timestamp = new Date();

    await locationSecurityCheck(t, privateDataRef, [latitude, longitude], timestamp);

    // Create pin object & calculate cost
    const pin: Pin = {
      caption,
      type,
      location: new GeoPoint(latitude, longitude),
      nearbyLocationName,
      broadLocationName,
      geohash: geohashForLocation([latitude, longitude]),
      authorUID: context.auth.uid,
      timestamp,
      finds: 0,
      cost: await calculateCost([latitude, longitude], t),
    };
    if (type === PinType.TEXT) pin.textContent = textContent;

    const metadata: PinMetadata = {
      cost: pin.cost,
      timestamp,
      broadLocationName,
      nearbyLocationName,
      pinSource: PinSource.SELF,
    };

    // Check user has sufficient currency
    const privateData = (await t.get(privateDataRef)).data();
    if (
      !privateData ||
      !privateData.currency ||
      privateData.currency < pin.cost
    ) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Insufficent funds."
      );
    }

    // Create activity to push
    const activity: Activity = {
      type: ActivityType.DROP,
      id: pinRef.id,
      author: context.auth.uid,
      timestamp,
      broadLocationName,
      nearbyLocationName,
    };

    // Deduct currency & create pin
    t.update(privateDataRef, { currency: privateData.currency - pin.cost });
    t.create(pinRef, pin);
    t.set(droppedRef, { [pinRef.id]: metadata }, { merge: true });
    t.update(userRef, { numPinsDropped: FieldValue.increment(1) });
    t.update(privateDataRef, { lastActivity: timestamp.valueOf() });
    t.update(privateDataRef, { lastLocation: [latitude, longitude] });
    t.set(
      activityRef,
      { activity: FieldValue.arrayUnion(activity) },
      { merge: true }
    );
  });
  return pinRef.id;
};

export const findPinHandler = async (
  { pid, latitude, longitude, pinSource },
  context
) => {
  // Validate auth status and args
  if (!context || !context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "dropPin must be called while authenticated."
    );
  }
  if (!pid || !latitude || !longitude || !pinSource) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "dropPin must be called with proper arguments."
    );
  }

  // Get document references for reading/writing
  const pinRef = firestore().collection("pins").doc(pid);
  const userRef = firestore().collection("users").doc(context.auth.uid);
  const foundRef = userRef.collection("found").doc(pid);
  const activityRef = userRef.collection("metadata").doc("activity");
  const privateDataRef = userRef.collection("metadata").doc("private");

  // Check user has not found pin yet
  const foundData = (await foundRef.get()).data();
  if (foundData !== undefined) {
    throw new functions.https.HttpsError(
      "already-exists",
      "You have already found this pin."
    );
  }

  // Perform pin read, reward calculation, & writes in one transaction
  return await firestore().runTransaction(async (t) => {
    const timestamp = new Date();

    await locationSecurityCheck(t, privateDataRef, [latitude, longitude], timestamp);

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
    if (
      distanceBetween(
        [latitude, longitude],
        [pinData.location.latitude, pinData.location.longitude]
      ) > PIN_FIND_RADIUS_KILOMETERS
    ) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Not close enough to find pin!"
      );
    }

    const reward = calculateReward(pinData);

    const metadata: PinMetadata = {
      reward,
      timestamp,
      nearbyLocationName: pinData.nearbyLocationName,
      broadLocationName: pinData.broadLocationName,
      pinSource,
    };

    // Create activity to push
    const activity: Activity = {
      type: ActivityType.FIND,
      id: pinRef.id,
      author: context.auth.uid,
      timestamp,
      nearbyLocationName: pinData.nearbyLocationName,
      broadLocationName: pinData.broadLocationName,
    };

    t.update(privateDataRef, { currency: FieldValue.increment(reward) });
    t.update(pinRef, { finds: FieldValue.increment(1) });
    t.update(userRef, { numPinsFound: FieldValue.increment(1) });
    t.update(privateDataRef, { lastActivity: timestamp.valueOf() });
    t.update(privateDataRef, { lastLocation: [latitude, longitude] });
    t.create(foundRef, metadata);
    t.set(
      activityRef,
      { activity: FieldValue.arrayUnion(activity) },
      { merge: true }
    );
    return {
      nearbyLocationName: pinData.nearbyLocationName,
      broadLocationName: pinData.broadLocationName,
      reward,
    };
  });
};

// checks speed based on diff between previous and current timestamp and location
// throws HTTP error if invalid speed is detected
// users with `isDev: true` in their private metadata will bypass this check
async function locationSecurityCheck(
  transaction: firestore.Transaction,
  privDataRef: firestore.DocumentReference<firestore.DocumentData>,
  currLoc: Geopoint,
  currTime: Date
) {
  // anti-spoof check
  const userData = (await transaction.get(privDataRef)).data();
  if (!userData) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "User does not exist."
    );
  }

  // developers get to skip location check
  if (userData.isDev) {
    return;
  }

  const lastActivity = new Date(userData.lastActivity);
  const lastLocation: Geopoint = userData.lastLocation;

  if (!antiSpoofCheck(
    lastLocation,
    currLoc,
    lastActivity,
    currTime)) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Location spoofing detected. You are moving suspiciously fast."
    );
  }
}

function antiSpoofCheck(
  prevLoc: Geopoint,
  currLoc: Geopoint,
  prevTimestamp: Date,
  currTimestamp: Date): boolean {

  const KILO_TO_MILE: number = 0.6213711;

  // so we don't break existing users
  if (!prevLoc || !prevTimestamp) {
    console.log("user does not have previous location / timestamp, passing check");
    return true;
  }

  const dist = KILO_TO_MILE * distanceBetween(prevLoc, currLoc);
  const interval = currTimestamp.valueOf() - prevTimestamp.valueOf();
  console.log(`curr: ${currTimestamp}, prev: ${prevTimestamp}`);
  const hours = (interval / 1000.0) / (60.0 * 60.0);
  const mph = dist / hours;
  let valid = false;

  console.log(`dist: ${dist}, interval: ${interval}, hours ${hours}`);

  if (dist < 1) {
    // can't walk faster than 5mph
    valid = mph < 5;
  } else if (dist < 5) {
    // can't bike faster than 20mph
    valid = mph < 25;
  } else if (dist < 1000) {
    // can't drive faster than 100mph
    valid = mph < 100;
  } else {
    // can't ride a plane faster than 650mph
    valid = mph < 650;
  }

  if (valid) {
    console.log(`${prevLoc} is close enough to ${currLoc} to reasonablly travel in ${hours} hrs`);
  } else {
    console.log(`${prevLoc} is too far from ${currLoc} to reasonablly travel in ${hours} hrs`);
  }

  return valid;
}
