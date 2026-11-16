# Exercise release boundaries without signing, authentication or network calls.
require 'tmpdir'
require 'fileutils'
$calls = []
module UI
  def self.user_error!(message); raise ArgumentError, message; end
end
def platform(*); yield; end
def desc(*); end
def lane(*); end
def import(path); load File.expand_path("../fastlane/#{path}", __dir__); end
%i[app_store_connect_api_key setup_ci update_code_signing_settings build_app upload_to_testflight upload_to_play_store].each do |name|
  define_method(name) { |**options| $calls << [name, options]; options }
end
def sh(*args); $calls << [:sh, args]; end
def match(**options)
  $calls << [:match, options]
  ENV['sigh_com.bargeboss.game.ios_appstore_profile-name'] = 'match AppStore com.bargeboss.game.ios'
end
load File.expand_path('../fastlane/Fastfile', __dir__)
def assert(value, message); raise message unless value; end
def rejects
  yield
rescue ArgumentError
  return
else
  raise 'Expected rejected configuration'
end
ENV.keys.grep(/\A(?:IOS_|MATCH_|GOOGLE_PLAY_|PLAY_RELEASE_STATUS)/).each { |k| ENV.delete(k) }
rejects { ios_archive! }
assert($calls.empty?, 'Missing signing inputs must fail before actions')
%w[IOS_TEAM_ID IOS_MATCH_GIT_URL IOS_MATCH_GIT_PRIVATE_KEY IOS_MATCH_PASSWORD IOS_APPSTORE_KEY_ID IOS_APPSTORE_ISSUER_ID IOS_APPSTORE_KEY_BASE64].each { |key| ENV[key] = 'fixture' }
ENV['IOS_BUILD_NUMBER'] = '42.100'
rejects { ios_archive! }
assert($calls.empty?, 'Invalid build number must fail before actions')
ENV['IOS_BUILD_NUMBER'] = '42.1'
ios_archive!
assert($calls.assoc(:match)[1][:readonly], 'CI must not create signing resources')
assert($calls.assoc(:match)[1][:app_identifier] == 'com.bargeboss.game.ios', 'Correct app signing profile')
assert($calls.assoc(:build_app)[1][:xcargs].include?('BARGEBOSS_IOS_PROFILE=0'), 'No developer fixtures in archive')
assert(!$calls.assoc(:upload_to_testflight), 'Archive must not upload')
assert(ENV['MATCH_PASSWORD'].nil? && ENV['MATCH_GIT_PRIVATE_KEY'].nil?, 'Restore match credential environment')
play_options = nil
Dir.mktmpdir do |root|
  rejects { ios_upload_testflight!(ipa_path: File.join(root, 'missing.ipa')) }
  ipa = File.join(root, 'BargeBoss.ipa'); File.write(ipa, 'fixture')
  ios_upload_testflight!(ipa_path: ipa)
  options = $calls.assoc(:upload_to_testflight)[1]
  assert(!options[:distribute_external], 'No external tester distribution')
  rejects { play_upload_options(root) }
  ENV['GOOGLE_PLAY_SERVICE_ACCOUNT_JSON'] = '[]'
  rejects { play_upload_options(root) }
  ENV['GOOGLE_PLAY_SERVICE_ACCOUNT_JSON'] = JSON.generate(type: 'service_account', client_email: 'fixture@example.invalid', private_key: 'fixture')
  rejects { play_upload_options(root) }
  FileUtils.mkdir_p(File.join(root, 'build')); File.write(File.join(root, 'build/BargeBoss.aab'), 'fixture')
  options = play_options = play_upload_options(root)
  assert(options[:package_name] == 'com.bargeboss.game', 'Correct Play package')
  assert(options[:track] == 'internal' && options[:release_status] == 'draft', 'Internal draft by default')
  ENV['PLAY_RELEASE_STATUS'] = 'completed'
  assert(play_upload_options(root)[:release_status] == 'completed', 'Explicit internal activation')
  ENV['PLAY_RELEASE_STATUS'] = 'production'
  rejects { play_upload_options(root) }
end
# Verify options against the pinned fastlane when dependencies are installed.
if ENV['BUNDLE_GEMFILE']
  require 'fastlane'
  %w[app_store_connect_api_key setup_ci match update_code_signing_settings build_app upload_to_testflight upload_to_play_store].each { |name| require "fastlane/actions/#{name}" }
  $calls.each do |name, options|
    next if name == :sh
    klass = Fastlane::Actions.const_get(name.to_s.split('_').map(&:capitalize).join + 'Action')
    assert((options.keys - klass.available_options.map(&:key)).empty?, "Unsupported #{name} options")
  end
  assert((play_options.keys - Fastlane::Actions::UploadToPlayStoreAction.available_options.map(&:key)).empty?, 'Unsupported Play options')
end
puts 'Release boundary checks passed (no credentials or uploads).'
