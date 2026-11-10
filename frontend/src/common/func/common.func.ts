export function money(value: number): string {
    return new Intl.NumberFormat("vi-VN").format(value) + " ₫";
}

export function formatScore(value: number | null): string {
    if (value === null) {
        return "–";
    }

    return new Intl.NumberFormat("vi-VN", {
        maximumFractionDigits: 2,
    }).format(value);
}

export function formatDate(value: string, withTime = false): string {
    return new Intl.DateTimeFormat(
        "vi-VN",
        withTime ? { dateStyle: "short", timeStyle: "short" } : undefined,
    ).format(new Date(value));
}

/*
 * Today as "yyyy-MM-dd" in local time, the value of a date input
 */
export function todayInputValue(): string {
    const now = new Date();

    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");

    return `${now.getFullYear()}-${month}-${day}`;
}
