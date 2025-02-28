# WebsiteCaptcha

WebsiteCaptcha is a Minecraft server plugin designed to enhance security by requiring players to verify themselves via a web-based CAPTCHA before accessing the main game world. This plugin integrates with a SQLite database for persistent verification data and offers extensive customization options to tailor the verification experience to your server’s needs.

## Disclaimer
WebsiteCaptcha is an experimental plugin and may contain numerous bugs and glitches. Developed as a proof-of-concept, it might exhibit instabilities, especially under high player loads or with certain server configurations. Features such as the embedded web server, SQLite database, and timeout mechanics could lead to unexpected behavior. Use this plugin at your own risk, and thoroughly test it in a controlled environment before deploying it on a live server. Contributions to fix bugs or enhance stability are highly encouraged!

## Plugin Information
WebsiteCaptcha protects your Minecraft server from unauthorized access by implementing a CAPTCHA verification system. Upon joining, unverified players are sent to a configurable "Limbo" world where they must complete a CAPTCHA challenge hosted on a local web server (default: `localhost:8080`). Once verified, players gain access to the main world. The plugin includes features like an XP bar countdown timer, configurable game modes, flight options, and visual effects like blindness to manage unverified players effectively.

### Key Features
- **CAPTCHA Verification**: Players complete a Google reCAPTCHA challenge via a web link.
- **Limbo World**: Unverified players are isolated in a separate world with restricted permissions.
- **XP Bar Timer**: Displays remaining verification time, decreasing smoothly until timeout.
- **Configurable Settings**: Customize timeout duration, game mode, flight, blindness, and inventory hiding via `config.yml`.
- **Kick Delay**: Displays a configurable message for 5 seconds before kicking on timeout.
- **Version Detection**: Logs player Minecraft versions (1.8 to 1.21.4) using ProtocolLib.
- **Persistent Storage**: Stores verification timestamps and states in SQLite (`verification.db`).

### How It Works
1. **Player Joins**: The server logs the player’s IP, name, and version, then checks their verification status.
2. **Verification Check**: If unverified or the grace period has expired, the player is sent to Limbo.
3. **CAPTCHA Prompt**: A clickable chat message provides a link to the CAPTCHA page.
4. **Timeout Mechanism**: An XP bar counts down; if time runs out, a kick message appears for 5 seconds before disconnection.
5. **Success**: Upon CAPTCHA completion, the player is teleported to the main world with restored settings.

## Installation
1. **Dependencies**:
   - **Bukkit/Spigot**: Compatible with versions 1.20 to 1.21.4.
   - **ProtocolLib**: Required for Limbo system and version detection (`getPlayerVersion`).
2. **Steps**:
   - Download the latest `WebsiteCaptcha.jar` from the [Releases](https://github.com/SalmanDev/WebsiteCaptcha/releases) page.
   - Place it in your server’s `plugins` folder.
   - Install ProtocolLib and ensure it’s loaded.
   - Start the server to generate `config.yml` in `plugins/WebsiteCaptcha/`.
   - Configure `config.yml` with your reCAPTCHA keys and preferences.

## Configuration
The plugin uses a `config.yml` file for customization. Below is an excerpt of key settings (see the full file in the repository):











For detailed configuration options, refer to the `config.yml` file in the repository.

## Contributing
This plugin is open-source under the GNU General Public License v3.0. We welcome contributions from the community to improve and grow WebsiteCaptcha! You can:

- Report bugs or suggest features via [Issues](https://github.com/SalmanDev/WebsiteCaptcha/issues).
- Fork the repository, make enhancements, and submit Pull Requests.
- Optimize performance, squash bugs, or add new features to enhance its reliability and utility.

Contributions must adhere to the GNU GPL v3.0, ensuring that any modifications remain open-source and freely available. Check out the repository, dive into the code, and help us make WebsiteCaptcha a robust security solution for Minecraft servers!

## License
WebsiteCaptcha is licensed under the GNU General Public License v3.0. This license grants you the freedom to use, study, share, and modify the software, provided any derivative works are distributed under the same terms. See the [LICENSE](https://github.com/SalmanDev/WebsiteCaptcha/blob/main/LICENSE) file for the full text.

Key aspects of GPL v3.0:
- **Freedom**: Use, modify, and distribute the plugin freely.
- **Copyleft**: Any changes or derived works must remain open-source under GPL v3.0.
- **No Warranty**: The plugin is provided "as is" with no guarantees of fitness or performance.

## Credits
- **Author**: SalmanDev (GitHub: [SalmanDev](https://github.com/SalmanDev))
- **Dependencies**:
  - ProtocolLib by dmulloy2 and contributors.

## Support
Encountered a bug? Have a feature idea? Open an [Issue](https://github.com/SalmanDev/WebsiteCaptcha/issues) on GitHub or join the discussion. Your input is crucial to refining this plugin. Let’s collaborate to secure Minecraft servers worldwide!
