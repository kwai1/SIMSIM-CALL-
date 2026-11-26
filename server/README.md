# SIMSIM CALL server

## Render configuration

- Root Directory: `server`
- Build Command: `npm install`
- Start Command: `npm start`
- Runtime: Node.js 20 or newer

Set these environment variables in Render. Do not place real credentials in GitHub:

- `FIREBASE_SERVICE_ACCOUNT_JSON`: the complete JSON contents of a Firebase service-account key for project `simsim-call-52f31`.
- `LIVEKIT_API_KEY`: LiveKit project API key.
- `LIVEKIT_API_SECRET`: LiveKit project API secret.
- `LIVEKIT_URL`: LiveKit project WebSocket URL, beginning with `wss://`.

After deployment, `GET /health` should return HTTP 200 with status `ready`.

The `POST /token` endpoint requires an Authorization header containing a Firebase ID token and a JSON body with `callId`. It checks that the Firebase account is active, that the account belongs to the requested call, and that the call has been accepted before minting a short-lived LiveKit token.

The server does not accept client-supplied room names, identities, or shared invitation codes. Call encryption keys are exchanged separately by the app and are never sent to this server.
