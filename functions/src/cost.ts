import * as functions from "firebase-functions";
import { GeoPoint } from "firebase-admin/firestore";

import { getPinsNearby, haversineDistance } from "./location";
import { Pin } from "./types";

export const BASE_COST = 50;

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
  return await calculateCost(new GeoPoint(latitude, longitude));
}

export async function calculateCost(dropLoc: GeoPoint) {
  const RADIUS = 2;
  const MAX_COEFF = 20;
  const SCALE = 4;

  const pins = await getPinsNearby(dropLoc.latitude, dropLoc.longitude, RADIUS);

  // cost algorithm:
  //
  // for every existing pin within RADIUS miles:
  //  calculate dist (haversine) between existing pin and potential pin
  //  calculate multiplier between 0.1 and MAX_COEFF (exponential decay with
  //  SCALE * distance, SCALE makes the decay steeper)
  //  add cost to running total (which is initalized to BASE)
  //
  //  essentially the idea is to make it:
  //  - prohibitively expensive to drop a pin right next to an existing one ($10+ in pinnies)
  //  - somewhat expensive to drop a pin that has a few neighbors within a few miles ($0.75 - $1.25)
  //  - cheap to drop a pin several miles away from any other pin ($0.1 - $0.2)
  return pins.map(d => {
    const p = d.data();

    const neighborLoc = new GeoPoint(
      p.location.latitude,
      p.location.longitude
    );

    const distance = haversineDistance(dropLoc, neighborLoc);

    const multiplier = ( MAX_COEFF ) / ( (MAX_COEFF - 1) * SCALE * distance + 1 )
    const cost = multiplier * BASE_COST;

    console.log(`distance: ${distance}, cost: ${cost}`);

    return Math.round(cost);
  }).reduce((total, n) => total + n, BASE_COST);
}

export function calculateReward(pin: Pin) {
  const DECAY_RATE = 0.6;
  const ogCost = pin.cost ? pin.cost : BASE_COST; // old pins don't have a cost field
  const reward = pin.finds > 15 // reward multiplier is like 0.01% once we have >= 15 rewards
    ? BASE_COST                 // so might as well just return the base reward
    : Math.round(ogCost * Math.pow(DECAY_RATE, pin.finds + 1) + BASE_COST);

  console.log(`finds: ${pin.finds}, cost: ${ogCost}, reward: ${reward}`);
  return reward;
}
