import { firestore } from "firebase-admin";
import * as functions from "firebase-functions";
import { GeoPoint } from "firebase-admin/firestore";

import { calculateCost, calculateReward } from "./cost";
import { Pin, PinType } from "./types";
import { haversineDistance, PIN_FIND_RADIUS_MILES } from "./location";

// TODO: add anti-spoof check before dropping pin
export const dropPinHandler = async ({ textContent, caption, type, latitude, longitude }, context) => {
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
    const cost = await calculateCost(new GeoPoint(latitude, longitude));

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
    const distanceMiles = haversineDistance(
      new GeoPoint(latitude, longitude),
      pinData.location
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
