import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

import { getNearbyPinsHandler } from "./location";
import { calcPinCostHandler } from "./cost";
import { dropPinHandler, findPinHandler, reportPinHandler } from "./pin";
import { deleteAccountHandler, deletePinHandler } from "./delete";

admin.initializeApp();

export const getNearbyPins = functions.https.onCall(getNearbyPinsHandler);
export const calcPinCost = functions.https.onCall(calcPinCostHandler);
export const dropPin = functions.https.onCall(dropPinHandler);
export const findPin = functions.https.onCall(findPinHandler);
export const deleteAccount = functions.https.onCall(deleteAccountHandler);
export const deletePin = functions.https.onCall(deletePinHandler);
export const reportPin = functions.https.onCall(reportPinHandler);
