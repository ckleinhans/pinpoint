import { GeoPoint } from "firebase-admin/firestore";
import * as functions from "firebase-functions";
import { firestore } from "firebase-admin";

export const EARTH_RADIUS_MILES = 3959;
export const ONE_MILE_LATITUDE_DEGREES = 0.014492753623188;
export const PIN_FIND_RADIUS_MILES = 0.0095; // 50 ft
export const NEARBY_PIN_RADIUS_MILES = 0.5;

export const toRadian = (angle: number) => (Math.PI / 180) * angle;
export const distance = (a: number, b: number) => (Math.PI / 180) * (a - b);

export const haversineDistance = (p1: GeoPoint, p2: GeoPoint) => {
  const [lat1, lon1] = [p1.latitude, p1.longitude];
  const [lat2, lon2] = [p2.latitude, p2.longitude];

  const dLat = distance(lat2, lat1);
  const dLon = distance(lon2, lon1);

  const lat1Rad = toRadian(lat1);
  const lat2Rad = toRadian(lat2);

  // Haversine Formula
  const a =
    Math.pow(Math.sin(dLat / 2), 2) +
    Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1Rad) * Math.cos(lat2Rad);
  const c = 2 * Math.asin(Math.sqrt(a));

  return EARTH_RADIUS_MILES * c;
};

export function calcOneMileLongitudeDegrees(latitude: number) {
  return 1 / ((Math.PI / 180) * EARTH_RADIUS_MILES * Math.cos(latitude));
}

export const getPinsNearby = async (
  latitude: number,
  longitude: number,
  radiusMiles: number,
  transaction?: firestore.Transaction
) => {
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

export const getNearbyPinsHandler = async ({ latitude, longitude }, context) => {
  // Validate auth status and args
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
