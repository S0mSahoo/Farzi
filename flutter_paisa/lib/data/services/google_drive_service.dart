import 'dart:convert';
import 'package:extension_google_sign_in_as_googleapis_auth/extension_google_sign_in_as_googleapis_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:googleapis/drive/v3.dart' as drive;
import '../models/backup_payload.dart';

class GoogleDriveService {
  static const String backupFileName = 'paisa_finance_backup.json';

  Future<drive.DriveApi?> _getDriveApi(GoogleSignInAccount account) async {
    final authClient = await account.authenticatedClient();
    if (authClient == null) return null;
    return drive.DriveApi(authClient);
  }

  /// Finds and loads the cloud backup payload from Google Drive AppData folder.
  Future<BackupPayload?> loadCloudData(GoogleSignInAccount account) async {
    try {
      final driveApi = await _getDriveApi(account);
      if (driveApi == null) return null;

      final fileList = await driveApi.files.list(
        spaces: 'appDataFolder',
        q: "name = '$backupFileName' and trashed = false",
        $fields: 'files(id, name, modifiedTime)',
      );

      final files = fileList.files;
      if (files == null || files.isEmpty) {
        return null;
      }

      final fileId = files.first.id;
      if (fileId == null) return null;

      final media = await driveApi.files.get(
        fileId,
        downloadOptions: drive.DownloadOptions.fullMedia,
      ) as drive.Media;

      final contentBytes = <int>[];
      await for (final chunk in media.stream) {
        contentBytes.addAll(chunk);
      }

      final jsonString = utf8.decode(contentBytes);
      final jsonMap = jsonDecode(jsonString) as Map<String, dynamic>;
      return BackupPayload.fromJson(jsonMap);
    } catch (e) {
      // Return null or rethrow based on caller
      return null;
    }
  }

  /// Idempotently saves or updates the cloud backup payload in Google Drive AppData folder.
  Future<int> saveCloudData(GoogleSignInAccount account, BackupPayload payload) async {
    final driveApi = await _getDriveApi(account);
    if (driveApi == null) throw Exception('Google Drive client authentication failed');

    final jsonString = jsonEncode(payload.toJson());
    final bytes = utf8.encode(jsonString);
    final media = drive.Media(Stream.value(bytes), bytes.length);

    final fileList = await driveApi.files.list(
      spaces: 'appDataFolder',
      q: "name = '$backupFileName' and trashed = false",
      $fields: 'files(id, name)',
    );

    final files = fileList.files;
    final now = DateTime.now().millisecondsSinceEpoch;

    if (files != null && files.isNotEmpty) {
      final fileId = files.first.id!;
      final driveFile = drive.File()..description = 'Paisa Cloud Backup';
      await driveApi.files.update(driveFile, fileId, uploadMedia: media);
    } else {
      final driveFile = drive.File()
        ..name = backupFileName
        ..parents = ['appDataFolder']
        ..description = 'Paisa Cloud Backup';
      await driveApi.files.create(driveFile, uploadMedia: media);
    }

    return now;
  }
}
