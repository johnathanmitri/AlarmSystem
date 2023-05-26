struct timeoutNode 
{
  unsigned long startTime;
  unsigned long delayDuration;  // because of the possibliilty of millis() overflow, we can't compare timestamps. We must store both the start time and the desired wait time. 
  void (*callbackFunction)();
  timeoutNode* next;
};

timeoutNode* rootNode;

void setTimeout(void (*callbackFunction)(), unsigned long delay) // like javascript setTimeout function
{
  timeoutNode* newNode = (timeoutNode*)malloc(sizeof(timeoutNode));
  newNode->startTime = millis();
  newNode->delayDuration = delay;
  newNode->callbackFunction = callbackFunction;
  
  // add the new node to the front of the linked list
  newNode->next = rootNode;
  rootNode = newNode;    
}

void checkTimeouts()
{
  timeoutNode* lastNode = NULL;// = rootNode;
  timeoutNode* currNode = rootNode;
  while (currNode != NULL)
  {
    if ((unsigned long)(millis() - currNode->startTime) > currNode->delayDuration)  //this protects against overflow issues when millis() gets to the max value. 
    {
      (*currNode->callbackFunction)(); // call the function
      
      // now remove this timeout from the list
      timeoutNode* nextNode = currNode->next;
      if (lastNode == NULL) // if this is the root node
        rootNode = nextNode;
      else
        lastNode->next = nextNode;
      
      free(currNode);
      currNode = nextNode;      
    }
    else
    {
      lastNode = currNode;
      currNode = currNode->next;
    }
  }
}

/*removeNode(timeoutNode* node)
{
  timeoutNode
}*/