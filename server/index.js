
import express from "express";
import cors from "cors";
import { AccessToken } from "livekit-server-sdk";

const app = express();

app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 3000;

app.get("/", (req, res) => {
  res.send("SIMSIM CALL SERVER ONLINE");
});

app.post("/token", async (req, res) => {
  try {
    const { identity, roomName, inviteCode } = req.body;

    if (!identity || !roomName || !inviteCode) {
      return res.status(400).json({
        error: "Missing required fields"
      });
    }

    if (inviteCode !== process.env.SIMSIM_INVITE_CODE) {
      return res.status(403).json({
        error: "Invalid invitation code"
      });
    }

    const apiKey = process.env.LIVEKIT_API_KEY;
    const apiSecret = process.env.LIVEKIT_API_SECRET;

    if (!apiKey || !apiSecret) {
      return res.status(500).json({
        error: "Server configuration error"
      });
    }

    const token = new AccessToken(apiKey, apiSecret, {
      identity,
      ttl: "1h"
    });

    token.addGrant({
      roomJoin: true,
      room: roomName,
      canPublish: true,
      canSubscribe: true
    });

    const jwt = await token.toJwt();

    res.json({
      token: jwt,
      serverUrl: process.env.LIVEKIT_URL
    });

  } catch (error) {
    console.error(error);
    res.status(500).json({
      error: "Token generation failed"
    });
  }
});

app.listen(PORT, () => {
  console.log("SIMSIM SERVER RUNNING ON PORT " + PORT);
});
