#c=1
#while [$? -eq 0]]; do
#  ++c
#  echo Running attempt $c
#  mvn clean install -pl testsuite/integration/manualmode -Dts.manualmode=true -Dtest=ReplicatedFailoverTestCase#testBackupActivation
#  if [$? -eq  0]; then
#      break;
#  fi
#done

i=1
while
      echo "Running attempt number  $i"
      i=`expr $i + 1`
      #: ${start=$i}              # capture the starting value of i
      ## some other commands      # needed for the loop
#      mvn clean install -pl testsuite/integration/manualmode -Dts.manualmode=true -Dtest=ReplicatedFailoverTestCase#testBackupActivation
mvn test -Dtest=RemoteLocalCallProfileTestCase

#testBackupFailoverAfterFailback
#testBackupActivation
      (($? == 0 ))             # Place the loop ending test here.
do :; done
echo "The loop was executed $i times "
