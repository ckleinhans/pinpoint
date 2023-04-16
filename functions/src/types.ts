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
  geohash: String;
  authorUID: string;
  timestamp: Date;
  finds: number;
  cost: number;
};

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
};
