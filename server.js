const { WebSocketServer } = require('ws');
const http = require('http');

// إنشاء سيرفر HTTP عادي لكي يقبله الموقع
const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('Screen Stream Server is Running!\n');
});

const wss = new WebSocketServer({ server });

wss.on('connection', (ws) => {
    console.log('New client connected');
    ws.on('message', (message) => {
        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === ws.OPEN) {
                client.send(message);
            }
        });
    });
});

server.listen(process.env.PORT || 3000, () => {
    console.log('Server listening...');
});
