import * as admin from "firebase-admin";
import { GeoPoint } from "firebase-admin/firestore";
import * as functions from "firebase-functions";

const EARTH_RADIUS_MILES = 3959;
const ONE_MILE_LATITUDE_DEGREES = 0.014492753623188;
const NUM_MILES_NEARBY = 2;

admin.initializeApp();

export const getNearbyPins = functions.https.onCall(
  async ({ latitude, longitude }, context) => {
    if (!isLoggedIn(context)) {
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

async function getPinsNearby(
  latitude: number,
  longitude: number,
  radiusMiles: number
) {
  const oneMileLongitudeDegrees =
    1 / ((Math.PI / 180) * EARTH_RADIUS_MILES * Math.cos(latitude));

  const lowerLat = latitude - ONE_MILE_LATITUDE_DEGREES * radiusMiles;
  const lowerLon = longitude - oneMileLongitudeDegrees * radiusMiles;

  const upperLat = latitude + ONE_MILE_LATITUDE_DEGREES * radiusMiles;
  const upperLon = longitude + oneMileLongitudeDegrees * radiusMiles;

  const lowerGeopoint = new GeoPoint(lowerLat, lowerLon);
  const upperGeopoint = new GeoPoint(upperLat, upperLon);

  const snapshot = await admin
    .firestore()
    .collection("pins")
    .where("location", ">", lowerGeopoint)
    .where("location", "<", upperGeopoint)
    .get();

  return snapshot.docs;
}

const isLoggedIn = (context: functions.https.CallableContext) =>
  context && context.auth && context.auth.uid;
