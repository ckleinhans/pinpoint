import { auth, firestore, storage } from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as functions from "firebase-functions";
const client = require("firebase-tools");

export const deleteAccountHandler = async (data, context) => {
  // Validate auth status and args
  if (!context || !context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "deleteAccount must be called while authenticated."
    );
  }

  const path = `users/${context.auth.uid}`;
  // Delete profile pic
  const bucket = storage().bucket();
  await bucket.file(path).delete({ ignoreNotFound: true });
  // Delete all user data
  await recursiveDelete(path);
  // Delete user authentication account
  await auth().deleteUser(context.auth.uid);
};

export const deletePinHandler = async ({ pid }, context) => {
  // Validate auth status and args
  if (!context || !context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "deletePin must be called while authenticated."
    );
  }
  if (!pid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "deletePin must be called with proper arguments."
    );
  }

  const userRef = firestore().collection("users").doc(context.auth.uid);
  const droppedRef = userRef.collection("dropped").doc("dropped");
  const pinRef = firestore().collection("pins").doc(pid);

  // Validate pin existence and author
  const pinData = await pinRef.get();
  if (!pinData.exists || !pinData.get("authorUID") === context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "deletePin must be called on a pin that you have dropped."
    );
  }

  // Delete pin image if it exists
  if (pinData.get("type") === "IMAGE") {
    console.log("deleting image");
    const bucket = storage().bucket();
    await bucket.file(`pins/${pid}`).delete();
  }
  
  // Delete all comments
  await recursiveDelete(`pins/${pid}/comments`);

  const batch = firestore().batch();
  batch.update(droppedRef, { [pinRef.id]: FieldValue.delete() });
  batch.update(userRef, { numPinsDropped: FieldValue.increment(-1) });
  batch.delete(pinRef);
  await batch.commit();
};

// Run a recursive delete on the given document or collection path.
const recursiveDelete = (path) =>
  client.firestore.delete(path, {
    project: process.env.GCLOUD_PROJECT,
    recursive: true,
    force: true,
  });
