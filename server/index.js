import express from "express";
import cors from "cors";
import rateLimit from "express-rate-limit";
import helmet from "helmet";
import { AccessToken } from "livekit-server-sdk";
import { cert, getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

const app = express();
const PORT = Number(process.env.PORT || 3000);

app.disable("x-powered-by");
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: "32kb" }));

const tokenLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: 12,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { error: "Too many token requests. Try again shortly." }
});

let adminAuth = null;
let firestore = null;

try {
  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!serviceAccountJson) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is not configured");
  }
  const serviceAccount = JSON.parse(serviceAccountJson);
  const firebaseApp = getApps().length
    ? getApps()[0]
    : initializeApp({ credential: cert(serviceAccount) });
  adminAuth = getAuth(firebaseApp);
  firestore = getFirestore(firebaseApp);
  console.log("Firebase Admin initialized");
} catch (error) {
  console.error("Firebase Admin initialization failed:", error.message);
}

app.get("/", (_req, res) => {
  res.status(200).send("SIMSIM CALL SERVER ONLINE");
});

app.get("/health", (_req, res) => {
  res.status(adminAuth && firestore ? 200 : 503).json({
    service: "simsim-call",
    status: adminAuth && firestore ? "ready" : "configuration_required"
  });
});

app.post("/token", tokenLimiter, async (req, res) => {
  try {
    if (!adminAuth || !firestore) {
      return res.status(503).json({ error: "Authentication service is not configured" });
    }

    const authorization = req.headers.authorization || "";
    const match = authorization.match(/^Bearer\s+(.+)$/i);
    if (!match) {
      return res.status(401).json({ error: "Firebase ID token is required" });
    }

    const decoded = await adminAuth.verifyIdToken(match[1], true);
    const uid = decoded.uid;
    const { callId } = req.body || {};

    if (typeof callId !== "string" || !/^[a-f0-9-]{20,80}$/i.test(callId)) {
      return res.status(400).json({ error: "Invalid call ID" });
    }

    const userSnapshot = await firestore.collection("users").doc(uid).get();
    if (!userSnapshot.exists || userSnapshot.get("status") !== "active") {
      return res.status(403).json({ error: "Account is not authorized" });
    }

    const callSnapshot = await firestore.collection("calls").doc(callId).get();
    if (!callSnapshot.exists) {
      return res.status(404).json({ error: "Call was not found" });
    }

    const call = callSnapshot.data();
    const isMember = call.callerUid === uid || call.calleeUid === uid;
    if (!isMember || call.status !== "accepted") {
      return res.status(403).json({ error: "You are not allowed to join this call" });
    }

    const otherUid = call.callerUid === uid ? call.calleeUid : call.callerUid;
    const otherUserSnapshot = await firestore.collection("users").doc(otherUid).get();
    if (!otherUserSnapshot.exists || otherUserSnapshot.get("status") !== "active") {
      return res.status(403).json({ error: "The other participant is not authorized" });
    }

    const createdAt = call.createdAt && typeof call.createdAt.toDate === "function"
      ? call.createdAt.toDate().getTime()
      : 0;
    if (!createdAt || Date.now() - createdAt > 2 * 60 * 60 * 1000) {
      return res.status(403).json({ error: "This call invitation has expired" });
    }

    const apiKey = process.env.LIVEKIT_API_KEY;
    const apiSecret = process.env.LIVEKIT_API_SECRET;
    const serverUrl = process.env.LIVEKIT_URL;

    if (!apiKey || !apiSecret || !serverUrl || !serverUrl.startsWith("wss://")) {
      return res.status(503).json({ error: "LiveKit server is not configured" });
    }

    if (typeof call.roomName !== "string" || !call.roomName.startsWith("simsim-")) {
      return res.status(400).json({ error: "Invalid room configuration" });
    }

    const token = new AccessToken(apiKey, apiSecret, {
      identity: uid,
      name: uid,
      ttl: "2h"
    });
    token.addGrant({
      roomJoin: true,
      room: call.roomName,
      canPublish: true,
      canSubscribe: true
    });

    return res.status(200).json({
      token: await token.toJwt(),
      serverUrl
    });
  } catch (error) {
    console.error("Token request failed:", error.message);
    return res.status(401).json({ error: "Unable to authorize this call" });
  }
});

app.listen(PORT, "0.0.0.0", () => {
  console.log("SIMSIM CALL SERVER listening on port " + PORT);
});
