SELECT name,value FROM stats WHERE value!=0 AND (severity='error' OR name GLOB '*lost*' OR name GLOB '*overrun*');
SELECT pid,name FROM process WHERE name GLOB '*bargeboss*';
SELECT count(*) AS gpu_slices FROM gpu_slice;
SELECT count(*) AS draws,min(ts),max(ts+dur) FROM slice WHERE name GLOB 'BB draw*';
SELECT t.name AS thread,s.name,count(*) AS n,round(sum(s.dur)/1e6,2) AS total_ms,round(avg(s.dur)/1e6,3) AS avg_ms,round(max(s.dur)/1e6,3) AS max_ms
FROM slice s JOIN thread_track tt ON tt.id=s.track_id JOIN thread t USING(utid) JOIN process p USING(upid)
WHERE p.name='com.bargeboss.game' AND s.dur>0 AND s.name NOT GLOB 'BB draw*'
GROUP BY t.name,s.name ORDER BY total_ms DESC LIMIT 65;
SELECT t.name,st.state,round(sum(st.dur)/1e6,2) AS ms FROM thread_state st JOIN thread t USING(utid) JOIN process p USING(upid) WHERE p.name='com.bargeboss.game' AND st.dur>0 GROUP BY t.name,st.state ORDER BY t.name,ms DESC;
SELECT name,type FROM track WHERE name GLOB '*gpu*' OR name GLOB '*GPU*';
