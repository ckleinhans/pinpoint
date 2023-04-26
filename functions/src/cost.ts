import * as functions from "firebase-functions";
import { Transaction } from "firebase-admin/firestore";
import { Geopoint, distanceBetween } from "geofire-common";

import { getPinsNearby } from "./location";
import { Pin } from "./types";

const BASE_COST = 50;

export const calcPinCostHandler = async ({ latitude, longitude }, context) => {
  if (!context || !context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "calcPinCost must be called while authenticated."
    );
  }
  if (!latitude || !longitude) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "calcPinCost must be called with a latitude and longitude."
    );
  }
  return await calculateCost([latitude, longitude]);
};

export async function calculateCost(
  dropLoc: Geopoint,
  transaction?: Transaction
) {
  const RADIUS_METERS = 1000;
  const MAX_COEFF = 50;
  const SCALE = 4;

  const pins = await getPinsNearby(dropLoc, RADIUS_METERS, transaction);

  // cost algorithm:
  //
  // for every existing pin within RADIUS_METERS:
  //  calculate dist (haversine) between existing pin and potential pin
  //  calculate multiplier between 0.1 and MAX_COEFF (exponential decay with
  //  SCALE * distance, SCALE makes the decay steeper)
  //  add cost to running total (which is initalized to BASE)
  //
  //  essentially the idea is to make it:
  //  - prohibitively expensive to drop a pin right next to an existing one ($10+ in pinnies)
  //  - somewhat expensive to drop a pin that has a few neighbors within a few miles ($0.75 - $1.25)
  //  - cheap to drop a pin several miles away from any other pin ($0.1 - $0.2)
  return pins
    .map((d) => {
      const p: Pin = <Pin>d.data();

      const distance = distanceBetween(dropLoc, [
        p.location.latitude,
        p.location.longitude,
      ]);

      const multiplier = MAX_COEFF / ((MAX_COEFF - 1) * SCALE * distance + 1);
      const cost = multiplier * BASE_COST;

      console.log(`distance: ${distance}, cost: ${cost}`);

      return Math.round(cost);
    })
    .reduce((total, n) => total + n, BASE_COST);
}

export function calculateReward(pin: Pin) {
  const DECAY_RATE = 0.6;
  const reward = Math.max(
    BASE_COST,
    Math.round(pin.cost * Math.pow(DECAY_RATE, pin.finds + 1))
  );

  console.log(`finds: ${pin.finds}, cost: ${pin.cost}, reward: ${reward}`);
  return reward;
}
