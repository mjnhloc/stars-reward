import { LoaderCircle } from "lucide-react";

export function LoadingScreen() {
    return (
        <div className="loading-screen">
            <LoaderCircle className="spinner" aria-hidden="true" />

            <span>Đang tải dữ liệu...</span>
        </div>
    );
}
