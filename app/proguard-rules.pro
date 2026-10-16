# ProGuard rules for BlueProof
# Keep Room entities
-keep class com.blueproof.data.db.entity.** { *; }
# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
