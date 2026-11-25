import { AccessToken } from "livekit-server-sdk";

export default async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
  if (req.method === "OPTIONS") return res.status(204).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  try {
    const { identity, roomName, inviteCode } = req.body || {};
    if (!identity || !roomName || !inviteCode) {
      return res.status(400).json({ error: "Missing required fields" });
    }
    if (!process.env.SIMSIM_INVITE_CODE || inviteCode !== process.env.SIMSIM_INVITE_CODE) {
      return res.status(403).json({ error: "Invalid invitation code" });
    }

    const { LIVEKIT_API_KEY, LIVEKIT_API_SECRET, LIVEKIT_URL } = process.env;
    if (!LIVEKIT_API_KEY || !LIVEKIT_API_SECRET || !LIVEKIT_URL) {
      return res.status(500).json({ error: "Server configuration error" });
    }

    const token = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
      identity: String(identity).slice(0, 128),
      ttl: "1h"
    });
    token.addGrant({
      roomJoin: true,
      room: String(roomName).slice(0, 128),
      canPublish: true,
      canSubscribe: true
    });
    return res.status(200).json({ token: await token.toJwt(), serverUrl: LIVEKIT_URL });
  } catch (error) {
    console.error("SIMSIM token error", error);
    return res.status(500).json({ error: "Token generation failed" });
  }
}
