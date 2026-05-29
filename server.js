wss.on('connection', (ws) => {
    ws.on('message', (data) => {
        // محاولة فحص الرسالة
        try {
            const messageString = data.toString();
            const parsed = JSON.parse(messageString);
            
            // إذا كانت الرسالة من نوع 'touch' أو 'text'، أرسلها للهاتف
            if (parsed.type === 'touch' || parsed.type === 'text') {
                wss.clients.forEach((client) => {
                    if (client !== ws && client.readyState === 1) {
                        client.send(messageString);
                    }
                });
            }
        } catch (e) {
            // إذا لم تكن JSON فهي صورة (Binary)، أرسلها للهاتف
            wss.clients.forEach((client) => {
                if (client !== ws && client.readyState === 1) {
                    client.send(data);
                }
            });
        }
    });
});
