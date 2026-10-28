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

export function formatDate(value: string): string {
    return new Intl.DateTimeFormat("vi-VN").format(new Date(value));
}
