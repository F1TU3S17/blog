function formatRussianDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return new Intl.DateTimeFormat("ru-RU", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}

document.querySelectorAll("[data-date]").forEach((element) => {
    element.textContent = formatRussianDate(element.dataset.date);
});
