import { firestore, storage } from "firebase-admin";
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

  const path = `/users/${context.auth.uid}`;
  // Delete profile pic
  const bucket = storage().bucket();
  await bucket.file(path).delete({ ignoreNotFound: true }).catch(console.error);
  // Delete all user data
  await recursiveDelete(path, process.env.FB_CI_TOKEN).catch(console.error);
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

  const batch = firestore().batch();

  batch.update(droppedRef, { [pinRef.id]: FieldValue.delete() });
  batch.update(userRef, { numPinsDropped: FieldValue.increment(-1) });
  batch.delete(pinRef);

  await batch.commit().catch(console.error);

  // Delete pin image if it exists
  const bucket = storage().bucket();
  await bucket
    .file(`/pins/${pid}`)
    .delete({ ignoreNotFound: true })
    .catch(console.error);
  // Delete all comments
  await recursiveDelete(`/pins/${pid}/comments`, process.env.FB_CI_TOKEN).catch(
    console.error
  );
};

// Run a recursive delete on the given document or collection path.
// The 'token' must be set in the functions config, and can be generated
// at the command line by running 'firebase login:ci'.
const recursiveDelete = (path, token) =>
  client.firestore.delete(path, {
    project: process.env.GCLOUD_PROJECT,
    recursive: true,
    force: true,
    token,
  });
