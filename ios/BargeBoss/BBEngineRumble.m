#import "BBEngineRumble.h"
#import <CoreHaptics/CoreHaptics.h>
#include <math.h>
#import "com/bargeboss/game/Game.h"

@implementation BBEngineRumble {
    CHHapticEngine *_engine;
    id<CHHapticAdvancedPatternPlayer> _player;
    float _strength;
    BOOL _failed;
}
- (void)update:(BBGame *)game feedback:(BOOL)feedback {
    if(@available(iOS 13.0,*)) {
        float strength=feedback?0:[game engineRumble];
        if(strength<.025f){[self stop];return;}
        if(_failed || !CHHapticEngine.capabilitiesForHardware.supportsHaptics)return;
        NSError *error=nil;
        if(!_engine){
            _engine=[[CHHapticEngine alloc] initAndReturnError:&error];
            if(!_engine){_failed=YES;return;}
            _engine.playsHapticsOnly=YES;
            __weak BBEngineRumble *weakSelf=self;
            _engine.stoppedHandler=^(CHHapticEngineStoppedReason reason){
                dispatch_async(dispatch_get_main_queue(), ^{BBEngineRumble *s=weakSelf;if(s){s->_player=nil;s->_strength=0;}});
            };
            _engine.resetHandler=^{dispatch_async(dispatch_get_main_queue(), ^{[weakSelf stop];});};
        }
        if(!_player){
            if(![_engine startAndReturnError:&error])return;
            CHHapticEvent *event=[[CHHapticEvent alloc] initWithEventType:CHHapticEventTypeHapticContinuous
                parameters:@[[[CHHapticEventParameter alloc] initWithParameterID:CHHapticEventParameterIDHapticIntensity value:1],
                             [[CHHapticEventParameter alloc] initWithParameterID:CHHapticEventParameterIDHapticSharpness value:.05]]
                relativeTime:0 duration:1];
            CHHapticPattern *pattern=[[CHHapticPattern alloc] initWithEvents:@[event] parameters:@[] error:&error];
            _player=[_engine createAdvancedPlayerWithPattern:pattern error:&error];
            _player.loopEnabled=YES;_player.loopEnd=1;
            [_player sendParameters:@[[[CHHapticDynamicParameter alloc] initWithParameterID:CHHapticDynamicParameterIDHapticIntensityControl value:strength relativeTime:0]] atTime:CHHapticTimeImmediate error:&error];
            if(![_player startAtTime:CHHapticTimeImmediate error:&error]){_player=nil;return;}
            _strength=strength;
        }else if(fabsf(strength-_strength)>.01f){
            [_player sendParameters:@[[[CHHapticDynamicParameter alloc] initWithParameterID:CHHapticDynamicParameterIDHapticIntensityControl value:strength relativeTime:0]] atTime:CHHapticTimeImmediate error:&error];
            _strength=strength;
        }
    }
}
- (void)stop {
    if(@available(iOS 13.0,*)) {[_player stopAtTime:CHHapticTimeImmediate error:nil];_player=nil;_strength=0;}
}
- (void)dealloc {[self stop];}
@end
