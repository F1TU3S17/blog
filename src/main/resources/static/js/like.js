const likePage = document.querySelector("[data-csrf-header]");

document.querySelectorAll(".like-button").forEach((button) => {
    button.addEventListener("click", async () => {
        if (button.dataset.authenticated !== "true") {
            window.location.href = button.dataset.loginUrl;
            return;
        }

        button.disabled = true;
        try {
            const { csrfToken, csrfHeader } = likePage.dataset;
            const response = await fetch(button.dataset.likeUrl, {
                method: button.getAttribute("aria-pressed") === "true" ? "DELETE" : "POST",
                headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {}
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            window.location.reload();
        } catch (error) {
            alert("Не удалось изменить лайк");
            button.disabled = false;
        }
    });
});
