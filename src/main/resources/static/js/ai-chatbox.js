document.addEventListener("DOMContentLoaded", function () {
    const chatToggle = document.getElementById("ai-chat-toggle");
    const chatBox = document.getElementById("ai-chat-box");
    const chatClose = document.getElementById("ai-chat-close");
    const chatInput = document.getElementById("ai-chat-input");
    const chatSend = document.getElementById("ai-chat-send");
    const chatMessages = document.getElementById("ai-chat-messages");

    if (!chatToggle || !chatBox) {
        console.error("Không tìm thấy nút chat hoặc khung chat");
        return;
    }

    chatToggle.addEventListener("click", function () {
        chatBox.classList.toggle("open");
        console.log("Đã bấm nút chat");
    });

    if (chatClose) {
        chatClose.addEventListener("click", function () {
            chatBox.classList.remove("open");
        });
    }

    if (chatSend) {
        chatSend.addEventListener("click", sendMessage);
    }

    if (chatInput) {
        chatInput.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                sendMessage();
            }
        });
    }

    function sendMessage() {
        const text = chatInput.value.trim();
        if (!text) return;

        addMessage(text, "user");
        chatInput.value = "";

        fetch("/api/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                message: text
            })
        })
        .then(response => response.json())
        .then(data => {
            addMessage(data.reply, "bot");
        })
        .catch(error => {
            addMessage("Có lỗi xảy ra khi gọi trợ lý AI.", "bot");
        });
    }

    function addMessage(text, type) {
        const div = document.createElement("div");
        div.className = "ai-message " + type;
        div.textContent = text;

        chatMessages.appendChild(div);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
});