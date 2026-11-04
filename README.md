# 📱 InstaSaver Pro — Ultimate Instagram Downloader & Ghost Explorer

एक शक्तिशाली, आधुनिक और 100% लीगल Android ऐप जो बिना मुख्य इंस्टाग्राम ऐप खोले या लॉगिन किए रील्स, पोस्ट्स, स्टोरीज और प्रोफाइल्स डाउनलोड और एक्सप्लोर करने की सुविधा देता है।

---

## 🌟 मुख्य विशेषताएं (Key Features)

### 1. 🔍 In-App Ghost Profile Explorer (बिना मुख्य ऐप के सर्च और डाउनलोड)
- ऐप में किसी का भी यूजरनेम सर्च करें (उदा: `@mrbeast`, `@virat.kohli`)।
- पूरी प्रोफाइल देखें: **Full HD DP (Zoomable)**, बायो, स्टैट्स।
- **Reels और Posts का 3-कॉलम ग्रिड** सीधे ऐप में ब्राउज़ करें।
- हर रील/फोटो पर 1-टैप डाउनलोड बटन।
- **"Download All" बल्क फीचर:** एक साथ उस क्रिएटर की सभी रील्स या सभी पोस्ट्स फोन में डाउनलोड करें।

### 2. 📥 All-in-One Media Downloader
- **Reels (1080p FHD):** ओरिजिनल ऑडियो और उच्चतम रिज़ॉल्यूशन के साथ।
- **Single Photos & Videos:** सामान्य फीड मीडिया का डायरेक्ट डाउनलोड।
- **Carousel Posts:** 10+ स्लाइड्स वाले पोस्ट्स के सभी मीडिया।
- **Lossless CDN Bitrate:** सीधे इंस्टाग्राम के मुख्य CDN सर्वर से ओरिजिनल बिटरेट पर डाउनलोड।

### 3. 🎵 ऑडियो एक्सट्रैक्शन (Metadata Supported)
- रील से सिर्फ ऑडियो अलग करना, जिसमें गाने का टाइटल, आर्टिस्ट और ओरिजिनल कवर आर्ट (M4A) शामिल रहता है।

### 4. ⚡ स्मार्ट यूज़र एक्सपीरियंस (UX & System Integration)
- **Manual Input Box + [PASTE] Button:** स्क्रीन पर हमेशा मौजूद।
- **Smart Clipboard Pill:** ऐप खोलते ही क्लिपबोर्ड में लिंक मिलते ही नीचे से फ्लोटिंग पिल।
- **Android "Share To..." Receiver:** इंस्टाग्राम में Share दबाकर सीधे ऐप पर भेजें और डाउनलोड शुरू करें।
- **1-Tap Caption Copier:** ओरिजिनल कैप्शन और हैशटैग्स कॉपी करने का बटन।

### 5. 🎬 इन-ऐप एचडी वीडियो प्लेयर और गैलरी
- **ExoPlayer Video Engine:** लूप प्लेबैक और स्मूथ कंट्रोल्स।
- **Auto-Creator Organization:** डाउनलोड्स `Movies/InstaSaver/[Creator]/` में सुरक्षित रूप से सेव होते हैं।
- **Smart Filters:** All, Videos, Photos, और क्रिएटर-वाइज़ फिल्टर।

---

## 📲 फोन में कैसे इंस्टॉल करें? (Installation Guide)

### तरीका 1: अपने एंड्रॉइड फोन पर सीधे इंस्टॉल करें
1. वर्कस्पेस में मौजूद APK फाइल को अपने फोन में भेजें:  
   👉 **[`InstaSaver-v1.0.apk`](file:///c:/Users/Dell/Downloads/new%20kipi%20devope/InstaSaver-v1.0.apk)**
2. फोन में फाइल मैनेजर खोलें और `InstaSaver-v1.0.apk` पर टैप करें।
3. *"Install from Unknown Sources"* को Allow करें।
4. ऐप तुरंत इंस्टॉल हो जाएगा और इस्तेमाल के लिए तैयार है!

### तरीका 2: एमुलेटर / ADB के जरिए इंस्टॉल करें
अगर आपका फोन USB केबल से जुड़ा है या एमुलेटर ऑन है, तो टर्मिनल में यह कमांड चलाएं:
```powershell
adb install -r InstaSaver-v1.0.apk
```

---

## 🛠️ टेक्निकल स्टैक (Tech Stack)
* **Language:** Kotlin 2.2 / 2.3
* **UI Framework:** Jetpack Compose + Material 3 Design
* **Networking & Scraping:** OkHttp 4.12, Jsoup 1.18, Kotlinx Serialization
* **Video Engine:** AndroidX Media3 (ExoPlayer 1.5.1)
* **Image Engine:** Coil Compose 2.7
* **Storage Safety:** Android MediaStore API (`IS_PENDING = 1` atomic write)
* **Target Android Version:** Android 8.0 (API 26) से लेकर Android 15/16 (API 36)

