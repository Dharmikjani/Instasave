# 🧪 InstaSaver Pro — Detailed Test Plan & Scenario Matrix (0% Hallucination)

**Test Plan Version:** 1.0  
**Methodology:** Deterministic Test-Driven Verification (Unit, Mock, Real-Device, and Network Fallback)

---

## 🎯 1. Test Objectives & Scope
यह टेस्ट प्लान यह सुनिश्चित करने के लिए है कि कोई भी फीचर केवल "दिखावे" या "मॉक" के लिए न हो, बल्कि **वास्तविक इंस्टाग्राम नेटवर्क और रियल एंड्रॉइड डिवाइसेज** पर शत-प्रतिशत काम करे।

---

## 📋 2. Comprehensive Test Scenarios (Matrix)

### मॉड्यूल 1: URL इनपुट, क्लिपबोर्ड और शेयर-शीट (Downloader Tab)

| Test ID | Test Scenario | Steps | Expected Result | Pass Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **TC-IN-01** | Standard Public Reel Link | इनपुट बॉक्स में `https://www.instagram.com/reel/C7xY9Z1A2B3/` डालें और Fetch दबाएं. | 1. शॉर्टकोड `C7xY9Z1A2B3` निकले.<br>2. थंबनेल, टाइटल और 1080p डाउनलोड बटन दिखे. | सही थंबनेल और MP4 लिंक लोड हो. |
| **TC-IN-02** | URL with Tracking Parameters | `https://instagram.com/reel/C7xY9Z1A2B3?igsh=MWx5YXAz&utm_source=copy` डालें. | ट्रैकिंग कोड साफ़ होकर ओरिजिनल शॉर्टकोड से मीडिया लोड हो. | कोई URL एरर न आए. |
| **TC-IN-03** | Multi-Slide Carousel Link | 10 फोटोज/वीडियो वाले पोस्ट का लिंक डालें. | कैरोसेल डिटेक्शन हो, सभी स्लाइड्स के थंबनेल रो में दिखें. | प्रत्येक स्लाइड डाउनलोड करने योग्य हो. |
| **TC-IN-04** | Invalid Domain / Non-IG Link | `https://facebook.com/reel/12345` या `https://youtube.com/watch` डालें. | अमान्य लिंक पर तुरंत एरर कार्ड: *"कृपया मान्य Instagram लिंक दर्ज करें"*. | ऐप क्रैश न हो, साफ़ एरर दिखे. |
| **TC-IN-05** | Clipboard Auto-Detect Pill | इंस्टाग्राम में लिंक कॉपी करें और ऐप खोलें. | स्क्रीन के नीचे फ्रॉस्टेड स्मार्ट पिल दिखे: `⚡ Reel link detected! [Download Now]`. | 1-क्लिक में डाउनलोड ट्रिगर हो. |
| **TC-IN-06** | Android Share Sheet | इंस्टाग्राम में Share दबाकर "InstaSaver" चुनें. | ऐप सीधे खुले, लिंक ऑटो-भर जाए और मीडिया फेच शुरू हो जाए. | यूजर को पेस्ट न करना पड़े. |

---

### मॉड्यूल 2: In-App Ghost Profile Explorer (प्रोफाइल सर्च और ग्रिड)

| Test ID | Test Scenario | Steps | Expected Result | Pass Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PR-01** | Public User Profile Search | यूजरनेम (उदा: `mrbeast`) डालें और Search दबाएं. | 1. बायो, फुल नेम, और फॉलोअर्स काउंट दिखे.<br>2. **Reels टैब में रील्स का ग्रिड दिखे (Empty नहीं!).**<br>3. Posts टैब में फोटोज का ग्रिड दिखे. | रील्स और पोस्ट्स दोनों ग्रिड में दिखने चाहिए। |
| **TC-PR-02** | Real User Profile (User's Own Account) | यूजर का अपना पब्लिक यूजरनेम सर्च करें. | प्रोफाइल की सभी पब्लिक रील्स और पोस्ट्स ग्रिड में लोड हों (302 लॉगिन रीडायरेक्ट से ब्लॉक न हो). | ग्रिड में थंबनेल्स दिखें। |
| **TC-PR-03** | Full HD DP Zoom Modal | अवतार फोटो पर टैप करें. | 1. फुलस्क्रीन डायलॉग में बड़ी एचडी फोटो खुले.<br>2. पिंच-टू-ज़ूम काम करे.<br>3. `Save HD DP` बटन दबाने पर फोटो गैलरी में सेव हो. | गैलरी में ओरिजिनल रेजोल्यूशन फोटो मिले. |
| **TC-PR-04** | Private Account Handling | प्राइवेट अकाउंट सर्च करें. | स्पष्ट सूचना दिखे: *"यह अकाउंट प्राइवेट है। देखने और डाउनलोड करने के लिए इन-ऐप लॉगिन करें।"* | खाली स्क्रीन के बजाय सही गाइडेंस मिले. |
| **TC-PR-05** | Download All Reels Button | प्रोफाइल पर `Download All` -> `Download All Reels` दबाएं. | सभी रील्स डाउनलोड कतार में लगें, 2-वर्कर सेमाफोर से एक-एक करके सेव हों. | सभी वीडियो गैलरी में आएं. |

---

### मॉड्यूल 3: डाउनलोड इंजन और मीडिया स्टोरेज (MediaStore & Gallery)

| Test ID | Test Scenario | Steps | Expected Result | Pass Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **TC-ST-01** | MediaStore Atomic Write (`IS_PENDING`) | 40MB का वीडियो डाउनलोड करें और बीच में इंटरनेट काटें. | गैलरी में कोई 0KB या करप्ट फाइल न दिखे. | केवल पूरा डाउनलोड होने पर फाइल गैलरी में दिखे. |
| **TC-ST-02** | Audio Extraction (M4A) | रील डाउनलोड कार्ड पर `Audio (M4A)` दबाएं. | वीडियो के अंदर का ओरिजिनल ऑडियो बिना री-कंप्रेशन के `Music/InstaSaver` में सेव हो. | फोन के म्यूजिक प्लेयर में गाना प्ले हो. |
| **TC-ST-03** | Low Storage Pre-Flight Check | फोन में 10MB स्पेस रहने पर 50MB वीडियो डाउनलोड करें. | डाउनलोड शुरू होने से पहले एरर: *"स्टोरेज कम है"*. | ऐप क्रैश न हो. |
| **TC-ST-04** | Auto-Folder Organization | क्रिएटर `viratkohli` का वीडियो डाउनलोड करें. | फाइल पाथ: `Movies/InstaSaver/viratkohli/Insta_viratkohli_[Date]_[Code].mp4`. | क्रिएटर के नाम से फोल्डर बने. |

---

### मॉड्यूल 4: इन-ऐप प्लेयर और वॉचलिस्ट

| Test ID | Test Scenario | Steps | Expected Result | Pass Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **TC-PL-01** | ExoPlayer Loop & Gestures | रील कार्ड पर `Play` दबाएं. | वीडियो कार्ड के अंदर ही बिना लैग के लूप में चले. | स्मूथ 60fps प्लेबैक. |
| **TC-WL-01** | Pin to Watchlist | प्रोफाइल पर `+ Watchlist` दबाएं. | क्रिएटर वॉचलिस्ट टैब में जुड़ जाए, ऐप रीस्टार्ट करने पर भी सेव रहे. | परसिस्टेंट स्टोरेज. |
| **TC-WL-02** | 1-Tap Open from Watchlist | वॉचलिस्ट में क्रिएटर पर टैप करें. | सीधे उसकी पूरी प्रोफाइल और रील्स ग्रिड खुल जाए (दोबारा सर्च न करना पड़े). | इंस्टेंट नेविगेशन. |

---

## 🔍 3. Verification Protocol (हम कैसे साबित करेंगे कि बग ठीक हो गया?)
1. **Network Interceptor Verification:** लॉग्स में HTTP स्टेटस कोड्स की जांच (302 Login Redirect के समय ऑटोमैटिक एम्बेड व स्क्रिप्ट पार्सर चलना चाहिए)।
2. **Real Profile Test:** यूजर के अपने इंस्टाग्राम अकाउंट का यूजरनेम डालकर रील्स और पोस्ट्स का ग्रिड प्रत्यक्ष रूप से लोड होना चाहिए।
3. **APK Execution Test:** अपडेटेड कोड का नया APK बिल्ड करके एमुलेटर और रियल डिवाइस दोनों पर टेस्ट करना।

