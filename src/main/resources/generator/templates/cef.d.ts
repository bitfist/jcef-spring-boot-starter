// src/types/cef.d.ts
export {};

declare global {
    interface Window {
        /**
         * Sends a query to the native side via CEF's message router.
         * @param params.request The request string payload.
         * @param params.persistent Whether the query persists until canceled.
         * @param params.onSuccess Called with the response string on success.
         * @param params.onFailure Called with error code and message on failure.
         * @returns A numeric request ID.
         */
        cefQuery(params: {
            request: string;
            persistent?: boolean;
            onSuccess?(response: string): void;
            onFailure?(errorCode: number, errorMessage: string): void;
        }): number;

        /**
         * Cancels a pending query returned by cefQuery.
         * @param requestId The ID returned by cefQuery.
         */
        cefQueryCancel?(requestId: number): void;
    }
}
