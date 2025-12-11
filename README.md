# Android WebView File Handler Library

[![](https://jitpack.io/v/mgks/android-webview-file-handler.svg)](https://jitpack.io/#mgks/android-webview-file-handler)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

A lightweight, robust library to handle file uploads (`<input type="file">`) inside Android WebViews. It handles the complex logic of `onShowFileChooser`, permissions, camera intents, and file providers automatically.

Extracted from the core of **[Android Smart WebView](https://github.com/mgks/Android-SmartWebView)**.

## Features
*   ✅ **Zero Boilerplate:** Handles `WebChromeClient` logic with one line.
*   ✅ **Camera Support:** Automatically offers Camera options in the chooser.
*   ✅ **Multiple Files:** Supports `multiple` attribute in HTML inputs.
*   ✅ **Secure:** Uses modern `FileProvider` to avoid `FileUriExposedException`.
*   ✅ **Kotlin & Java:** Written in Kotlin but 100% Java-friendly.

## Installation

**Step 1. Add the JitPack repository to your build file**

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```
**Gradle (Groovy):**
```groovy
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

**Step 2. Add the dependency**

```groovy
dependencies {
    implementation 'com.github.mgks:android-webview-file-handler:1.0.0'
}
```

*(Note: Replace `1.0.0` with the latest release tag)*

## Usage

### 1. In your Activity

Initialize the `SwvFileChooser` and hook it into your WebView's `WebChromeClient`.

**Kotlin:**
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var fileChooser: SwvFileChooser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webview)
        
        // 1. Initialize
        fileChooser = SwvFileChooser(this)

        webView.webChromeClient = object : WebChromeClient() {
            // 2. Hook into onShowFileChooser
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return fileChooser.onShowFileChooser(filePathCallback!!, fileChooserParams!!)
            }
        }
    }

    // 3. Pass the result back
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fileChooser.onActivityResult(requestCode, resultCode, data)
    }
}
```

**Java:**
```java
public class MainActivity extends AppCompatActivity {
    private SwvFileChooser fileChooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView webView = findViewById(R.id.webview);
        
        // 1. Initialize
        fileChooser = new SwvFileChooser(this);

        webView.setWebChromeClient(new WebChromeClient() {
            // 2. Hook into onShowFileChooser
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return fileChooser.onShowFileChooser(filePathCallback, fileChooserParams);
            }
        });
    }

    // 3. Pass the result back
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fileChooser.onActivityResult(requestCode, resultCode, data);
    }
}
```

### 2. Permissions

The library handles the logic, but you must declare the permissions in your app's `AndroidManifest.xml` if you want Camera support:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<!-- Required for API 33+ (Android 13) if accessing gallery images -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

**Important:** You must use ActivityCompat.requestPermissions or the `AndroidX Activity Result API` to request these permissions before the user tries to upload a file.

## Configuration (Optional)

You can customize the behavior by passing a `Config` object.

```kotlin
val config = SwvFileChooser.Config(
    allowCamera = true,      // Default: true
    allowGallery = true,     // Default: true
    allowMultiple = false    // Default: true
)
val fileChooser = SwvFileChooser(this, config)
```

## License
MIT License