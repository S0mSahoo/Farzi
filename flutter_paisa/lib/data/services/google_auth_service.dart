import 'package:google_sign_in/google_sign_in.dart';
import 'package:googleapis/drive/v3.dart' as drive;
import '../models/user_profile.dart';

class GoogleAuthService {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    scopes: [
      drive.DriveApi.driveAppdataScope,
      'email',
      'profile',
    ],
  );

  GoogleSignInAccount? _currentUser;
  GoogleSignInAccount? get currentUser => _currentUser;

  Stream<GoogleSignInAccount?> get onAuthStateChanged => _googleSignIn.onCurrentUserChanged;

  Future<UserProfile?> signInSilently() async {
    try {
      _currentUser = await _googleSignIn.signInSilently();
      if (_currentUser != null) {
        return UserProfile(
          name: _currentUser!.displayName ?? '',
          email: _currentUser!.email,
          photoUrl: _currentUser!.photoUrl ?? '',
          hasCompletedOnboarding: true,
        );
      }
    } catch (e) {
      // Ignored for silent sign-in
    }
    return null;
  }

  Future<UserProfile?> signIn() async {
    try {
      _currentUser = await _googleSignIn.signIn();
      if (_currentUser != null) {
        return UserProfile(
          name: _currentUser!.displayName ?? '',
          email: _currentUser!.email,
          photoUrl: _currentUser!.photoUrl ?? '',
          hasCompletedOnboarding: true,
        );
      }
    } catch (e) {
      rethrow;
    }
    return null;
  }

  Future<void> signOut() async {
    _currentUser = null;
    await _googleSignIn.signOut();
  }

  Future<GoogleSignInAccount?> getAuthenticatedAccount() async {
    if (_currentUser == null) {
      _currentUser = await _googleSignIn.signInSilently();
    }
    return _currentUser;
  }
}
