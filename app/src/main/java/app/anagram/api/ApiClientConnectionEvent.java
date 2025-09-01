package app.anagram.api;

import androidx.annotation.NonNull;

public abstract class ApiClientConnectionEvent {

    public static class Open extends ApiClientConnectionEvent {
        @NonNull
        @Override
        public String toString() {
            return "Open";
        }
    }

    public static class Message extends ApiClientConnectionEvent {
        public String message;

        public Message(String message) {
            this.message = message;
        }

        @NonNull
        @Override
        public String toString() {
            return "Message";
        }
    }

    public static class Close extends ApiClientConnectionEvent {
        @NonNull
        @Override
        public String toString() {
            return "Close";
        }
    }

    public static class Error extends ApiClientConnectionEvent {
        private final Throwable throwable;

        public Error(Throwable throwable) {
            this.throwable = throwable;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        @NonNull
        @Override
        public String toString() {
            return "Error: " + throwable.getMessage();
        }
    }

}