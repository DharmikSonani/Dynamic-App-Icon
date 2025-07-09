import { Text, View } from 'react-native'
import React from 'react'
import { NativeModules } from 'react-native';

console.log(NativeModules);

const App = () => {
  return (
    <View style={{
      flex: 1,
      justifyContent: 'center',
      alignItems: 'center',
    }}>
      <Text>App</Text>
    </View>
  )
}

export default App