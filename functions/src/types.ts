import { GeoPoint } from "firebase-admin/firestore";

export enum PinType {
  TEXT = "TEXT",
  IMAGE = "IMAGE",
}

export type Pin = {
  caption: string;
  textContent?: string;
  type: PinType;
  location: GeoPoint;
  nearbyLocationName: string;
  broadLocationName: string;
  geohash: String;
  authorUID: string;
  timestamp: Date;
  finds: number;
  cost: number;
};

export type PinMetadata = {
  cost?: number;
  reward?: number;
  timestamp: Date;
  broadLocationName: String;
  nearbyLocationName: String;
}

export enum ActivityType {
  DROP = "DROP",
  FIND = "FIND",
  COMMENT = "COMMENT",
  FOLLOW = "FOLLOW",
}

export type Activity = {
  type: ActivityType;
  id: string;
  author: string;
  timestamp: Date;
  broadLocationName?: String;
  nearbyLocationName?: String;
};
