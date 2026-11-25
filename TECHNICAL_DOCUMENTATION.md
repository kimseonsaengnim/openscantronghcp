# Technical Documentation

## Implemented foundation

The app uses a single Android module, Compose UI, Hilt injection, Room persistence, and Kotlin coroutines. `Examination`, `AnswerKey`, `Student`, and `Submission` are stored locally. Answer keys are separate versioned rows; a submission stores both its recognized/manual input and the final values, alongside the key version used for grading.

The grading engine is pure Kotlin. Each item is classified as correct, incorrect, blank, multiple-mark, or pending review. Scores are computed only for determinate responses. The maximum possible score is protected against zero, and score percentage is omitted if the maximum is zero.

## CSV

CSV cells are escaped per RFC 4180: cells containing commas, quotes, or line breaks are quoted, and quotes inside quoted cells are doubled. Output is UTF-8. Export is generated from persisted exam and submission records and written using Android's Storage Access Framework; the user selects the destination.

## Not yet implemented

No OMR layout or image-recognition algorithm is currently shipped. The planned sheet coordinate system will be normalized to the four registration-marker centroids. Future alignment should use fiducial geometry and a perspective transform before analyzing each bubble ROI against its local unmarked baseline. These stages require implementation and validation against a labeled dataset before accuracy can be reported.

Individual and batch PDF archiving, checksums, audit CSVs, backup/restore, CameraX, OpenCV, QR, and storage lifecycle policies are also pending. No claim is made that the app currently generates or archives scanned sheets.
