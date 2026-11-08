const postPage = document.getElementById("post-page");
const { postId, postsUrl, csrfToken, csrfHeader } = postPage.dataset;

document.querySelectorAll(".comment-delete-form").forEach((form) => {
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!confirm("Удалить комментарий?")) return;

        const button = form.querySelector("button");
        button.disabled = true;
        try {
            const response = await fetch(form.action, {
                method: "DELETE",
                headers: csrfToken ? { [csrfHeader]: csrfToken } : {}
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            window.location.reload();
        } catch (error) {
            alert("Не удалось удалить комментарий");
            button.disabled = false;
        }
    });
});

function showEditForm() {
    document.getElementById("post-view").classList.add("hidden");
    document.getElementById("edit-section").classList.remove("hidden");
}

function hideEditForm() {
    document.getElementById("edit-section").classList.add("hidden");
    document.getElementById("post-view").classList.remove("hidden");
}

async function deletePost() {
    if (!confirm("Вы уверены, что хотите удалить пост?")) {
        return;
    }

    try {
        const response = await fetch(`${postsUrl}/${postId}`, {
            method: "DELETE",
            headers: csrfToken ? { [csrfHeader]: csrfToken } : {}
        });

        if (!response.ok) {
            alert(`Ошибка удаления: ${response.status}`);
            return;
        }

        window.location.href = postsUrl;
    } catch (error) {
        alert("Не удалось удалить пост");
    }
}
