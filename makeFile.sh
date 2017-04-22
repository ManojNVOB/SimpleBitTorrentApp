
javac *.java

java PeerProcess 1001 &
echo "peer1 up"
sleep 1
java PeerProcess 1002 &
echo "peer2 up"
sleep 1
java PeerProcess 1003 &
echo "peer3 up"
sleep 1
java PeerProcess 1004 &
echo "peer4 up"

wait

echo "finished"
