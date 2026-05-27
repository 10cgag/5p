wss.on('connection', (ws) => {
    ws.on('message', (data) => {
        try {
            const message = JSON.parse(data);
            // إذا كانت رسالة لمس، أرسلها فقط للمتصلين الذين ليسوا متصفحات (أي الأندرويد)
            if (message.type === 'touch') {
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(data);
                    }
                });
            }
        } catch (e) {
            // إذا لم تكن JSON (أي أنها صورة)، أرسلها للجميع كالمعتاد
            wss.clients.forEach((client) => {
                if (client !== ws && client.readyState === 1) {
                    client.send(data);
                }
            });
        }
    });
});
