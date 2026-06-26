# ADR 0002: Material 3 and Dynamic Color Migration

## Status
Proposed

## Context
The Gautimes app and its accompanying home screen widget currently use a mix of hardcoded colors and early Material 3 (Material Expressive) implementations. To provide a first-class Android experience, we need to support system-wide theme changes (Light/Dark mode) and User-specific wallpaper colors (Dynamic Color/Material You), while preserving the functional "station display" branding.

## Decision
We will migrate the entire application and the Glance-based widget to a standard Material 3 implementation with full Dynamic Color support.

### Key Implementation Details:
1.  **Dynamic Color Priority**: On Android 12 (API 31) and above, the app and widget will use the user's wallpaper-generated color palette via `dynamicLightColorScheme` and `dynamicDarkColorScheme`.
2.  **Gautrain Fallback Palette**: For devices running Android 11 and below, we will use a custom-built Material 3 palette inspired by the Gautrain brand colors (Gold/Yellow and Blue).
3.  **Glance Theme Integration**: The home screen widget will be wrapped in `GlanceTheme`, allowing it to use `GlanceTheme.colors` for backgrounds and secondary text, ensuring it stays in sync with the system theme.
4.  **Functional Branding**: We will preserve the `amberLED` color for the "Departure Countdown" digits. This is a functional brand choice that mimics physical train station displays and will remain constant regardless of the theme to ensure high visibility and brand recognition.
5.  **Typography Standardization**: We will move away from custom "Emphasized" typography tokens and adopt standard Material 3 Typography tokens (`headlineSmall`, `labelLarge`, etc.) to ensure better scaling and system integration.

## Consequences
- **Positive**: The app will feel native to the user's device settings.
- **Positive**: High accessibility support through standard M3 tokens.
- **Positive**: Reduced maintenance of custom color logic.
- **Neutral**: The "Amber LED" color will provide a consistent brand anchor across all theme variations.
- **Negative**: Visual consistency across different devices will vary more due to Dynamic Color.
