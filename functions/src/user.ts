import * as functions from "firebase-functions";
const client = require("firebase-tools");
//import client = require('firebase-tools');

export const deleteAccountHandler = async (data, context) => {
  // Validate auth status and args
  if (!context || !context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "deleteAccount must be called while authenticated."
    );
  }

  const path = `/users/${context.auth.uid}`;

  // Run a recursive delete on the given document or collection path.
  // The 'token' must be set in the functions config, and can be generated
  // at the command line by running 'firebase login:ci'.
  await client.firestore.delete(path, {
    project: process.env.GCLOUD_PROJECT,
    recursive: true,
    force: true,
    token: process.env.FB_CI_TOKEN,
  });
};
