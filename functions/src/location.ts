import * as functions from "firebase-functions";
import { firestore } from "firebase-admin";
import { distanceBetween, geohashQueryBounds, Geopoint } from "geofire-common";
import { GeoPoint } from "firebase-admin/firestore";

const NEARBY_PIN_RADIUS_METERS = 1000;

// export const toRadian = (angle: number) => (Math.PI / 180) * angle;
// export const distance = (a: number, b: number) => (Math.PI / 180) * (a - b);

// export const haversineDistance = (p1: GeoPoint, p2: GeoPoint) => {
//   const [lat1, lon1] = [p1.latitude, p1.longitude];
//   const [lat2, lon2] = [p2.latitude, p2.longitude];

//   const dLat = distance(lat2, lat1);
//   const dLon = distance(lon2, lon1);

//   const lat1Rad = toRadian(lat1);
//   const lat2Rad = toRadian(lat2);

//   // Haversine Formula
//   const a =
//     Math.pow(Math.sin(dLat / 2), 2) +
//     Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1Rad) * Math.cos(lat2Rad);
//   const c = 2 * Math.asin(Math.sqrt(a));

//   return EARTH_RADIUS_MILES * c;
// };

export const getPinsNearby = async (
  location: Geopoint,
  radiusMeters: number,
  transaction?: firestore.Transaction
) => {
  // Each item in 'bounds' represents a startAt/endAt pair. We have to issue
  // a separate query for each pair. There can be up to 9 pairs of bounds
  // depending on overlap, but in most cases there are 4.
  const bounds = geohashQueryBounds(location, radiusMeters);
  const queries = bounds.map((b) =>
    transaction
      ? transaction.get(
          firestore()
            .collection("pins")
            .orderBy("geohash")
            .startAt(b[0])
            .endAt(b[1])
        )
      : firestore()
          .collection("pins")
          .orderBy("geohash")
          .startAt(b[0])
          .endAt(b[1])
          .get()
  );

  // Wait for all queries to complete
  const snapshots = await Promise.all(queries);

  // Get all documents & filter false positives (due to geohash accuracy)
  return snapshots
    .flat()
    .map((snap) => snap.docs)
    .flat()
    .filter((doc) => {
      const pinLoc: GeoPoint = doc.get("location");
      return (
        distanceBetween(location, [pinLoc.latitude, pinLoc.longitude]) * 1000 <=
        radiusMeters
      );
    });
};

export const getNearbyPinsHandler = async (
  { latitude, longitude },
  context
) => {
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
    [latitude, longitude],
    NEARBY_PIN_RADIUS_METERS
  );
  return Object.fromEntries(pins.map((pin) => [pin.id, pin.data().location]));
};
