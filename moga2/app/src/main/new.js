const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const path = require('path');
const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// THIS IS THE FIX: This line tells the browser to show the viewer, NOT the text message.
app.get('/', (req, res) => { res.sendFile(path.join(__dirname, 'index.html')); });

wss.on('connection', (ws) => {
    ws.on('message', (data) => {
        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === WebSocket.OPEN) {
                client.send(data);
            }
        });
    });
});
server.listen(process.env.PORT || 3000);