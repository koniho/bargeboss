#import "BBEngineSound.h"
#import <AVFoundation/AVFoundation.h>
#import "com/bargeboss/game/EngineAudio.h"
#import "com/bargeboss/game/Game.h"

@implementation BBEngineSound {
    AVAudioEngine *_engine;
    NSArray<AVAudioPlayerNode *> *_players;
    NSArray<AVAudioUnitVarispeed *> *_rates;
    NSArray<AVAudioPCMBuffer *> *_buffers;
    BOOL _playing,_interrupted,_failed;
}
- (instancetype)init {
    if((self=[super init])) {
        _engine=[[AVAudioEngine alloc] init];
        NSMutableArray *players=[NSMutableArray array],*rates=[NSMutableArray array],*buffers=[NSMutableArray array];
        for(int i=0;i<2;i++) {
            NSString *path=[@"assets/" stringByAppendingString:[BBEngineAudio assetWithInt:i]];
            NSURL *url=[NSBundle.mainBundle.resourceURL URLByAppendingPathComponent:path];
            NSError *error=nil;
            AVAudioFile *file=[[AVAudioFile alloc] initForReading:url error:&error];
            if(!file){NSLog(@"Engine sound unavailable: %@",error);_failed=YES;break;}
            AVAudioPCMBuffer *buffer=[[AVAudioPCMBuffer alloc] initWithPCMFormat:file.processingFormat frameCapacity:(AVAudioFrameCount)file.length];
            if(![file readIntoBuffer:buffer error:&error]){NSLog(@"Engine sound decode failed: %@",error);_failed=YES;break;}
            AVAudioPlayerNode *player=[[AVAudioPlayerNode alloc] init];
            AVAudioUnitVarispeed *rate=[[AVAudioUnitVarispeed alloc] init];
            [_engine attachNode:player];[_engine attachNode:rate];
            [_engine connect:player to:rate format:buffer.format];
            [_engine connect:rate to:_engine.mainMixerNode format:buffer.format];
            [players addObject:player];[rates addObject:rate];[buffers addObject:buffer];
        }
        _players=players;_rates=rates;_buffers=buffers;[_engine prepare];
        [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(interruption:) name:AVAudioSessionInterruptionNotification object:nil];
    }
    return self;
}
- (void)interruption:(NSNotification *)notification {
    if([notification.userInfo[AVAudioSessionInterruptionTypeKey] integerValue]==AVAudioSessionInterruptionTypeBegan){_interrupted=YES;[self stop];}
    else _interrupted=([notification.userInfo[AVAudioSessionInterruptionOptionKey] unsignedIntegerValue]&AVAudioSessionInterruptionOptionShouldResume)==0;
}
- (void)update:(BBGame *)game {
    if(![BBEngineAudio audibleWithBBGame:game]){[self stop];_interrupted=NO;return;}
    if(_failed || _interrupted)return;
    if(!_playing){
        NSError *error=nil;
        AVAudioSession *session=AVAudioSession.sharedInstance;
        if(![session setCategory:AVAudioSessionCategoryAmbient error:&error] || ![session setActive:YES error:&error] || ![_engine startAndReturnError:&error]){NSLog(@"Engine audio start failed: %@",error);_interrupted=YES;return;}
        for(int i=0;i<2;i++){
            _players[i].volume=[BBEngineAudio gainWithBBGame:game withInt:i];
            _rates[i].rate=[BBEngineAudio rateWithBBGame:game withInt:i];
            [_players[i] scheduleBuffer:_buffers[i] atTime:nil options:AVAudioPlayerNodeBufferLoops completionHandler:nil];
            [_players[i] play];
        }
        _playing=YES;
    }
    for(int i=0;i<2;i++){_players[i].volume=[BBEngineAudio gainWithBBGame:game withInt:i];_rates[i].rate=[BBEngineAudio rateWithBBGame:game withInt:i];}
}
- (void)stop {
    if(!_playing)return;
    for(AVAudioPlayerNode *player in _players)[player stop];
    [_engine pause];_playing=NO;
    [AVAudioSession.sharedInstance setActive:NO withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation error:nil];
}
- (void)dealloc {[NSNotificationCenter.defaultCenter removeObserver:self];[self stop];}
@end
