import { firestore } from "firebase-admin";
import { FieldValue, GeoPoint } from "firebase-admin/firestore";
import * as functions from "firebase-functions";
import { distanceBetween, geohashForLocation } from "geofire-common";

import { calculateCost, calculateReward } from "./cost";
import { Activity, ActivityType, Pin, PinType } from "./types";

const PIN_FIND_RADIUS_KILOMETERS = 0.02; // 20 meters

// TODO: add anti-spoof check before dropping pin
export const dropPinHandler = async (
  { textContent, caption, type, latitude, longitude },
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
  const privateDataRef = firestore()
    .collection("private")
    .doc(context.auth.uid);
  const pinRef = firestore().collection("pins").doc();
  const userRef = firestore().collection("users").doc(context.auth.uid);
  const droppedRef = userRef.collection("dropped").doc(pinRef.id);
  const activityRef = firestore().collection("activity").doc(context.auth.uid);

  // Perform validation & updates in transaction to enforce all or none
  await firestore().runTransaction(async (t) => {
    // Create pin object & calculate cost
    const pin: Pin = {
      caption,
      type,
      location: new GeoPoint(latitude, longitude),
      geohash: geohashForLocation([latitude, longitude]),
      authorUID: context.auth.uid,
      timestamp: new Date(),
      finds: 0,
      cost: await calculateCost([latitude, longitude], t),
    };
    if (type === PinType.TEXT) pin.textContent = textContent;

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
      timestamp: new Date(),
    };

    // Deduct currency & create pin
    t.update(privateDataRef, { currency: privateData.currency - pin.cost });
    t.create(pinRef, pin);
    t.create(droppedRef, { cost: pin.cost, timestamp: new Date() });
    t.update(userRef, { numPinsDropped: FieldValue.increment(1) });
    t.set(
      activityRef,
      { activity: FieldValue.arrayUnion(activity) },
      { merge: true }
    );
  });
  return pinRef.id;
};

// TODO: add anti-spoof check before finding pin
export const findPinHandler = async ({ pid, latitude, longitude }, context) => {
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
  const userRef = firestore().collection("users").doc(context.auth.uid);
  const foundRef = userRef.collection("found").doc(pid);
  const activityRef = firestore().collection("activity").doc(context.auth.uid);

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

    // Create activity to push
    const activity: Activity = {
      type: ActivityType.FIND,
      id: pinRef.id,
      author: context.auth.uid,
      timestamp: new Date(),
    };

    const reward = calculateReward(pinData);
    t.update(privateDataRef, { currency: FieldValue.increment(reward) });
    t.update(pinRef, { finds: FieldValue.increment(1) });
    t.update(userRef, { numPinsFound: FieldValue.increment(1) });
    t.create(foundRef, { reward, timestamp: new Date() });
    t.set(
      activityRef,
      { activity: FieldValue.arrayUnion(activity) },
      { merge: true }
    );
    return pinData;
  });
};
