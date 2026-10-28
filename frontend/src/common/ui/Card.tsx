export function Card({
    icon,
    label,
    value,
}: {
    icon: React.ReactNode;
    label: string;
    value: string;
}) {
    return (
        <article className="card">
            <div>
                <span>{label}</span>
                <strong>{value}</strong>
            </div>
            <i>{icon}</i>
        </article>
    );
}
