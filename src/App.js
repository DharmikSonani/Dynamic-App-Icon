import { Text, View } from 'react-native'
import React from 'react'
import { useDefaultIcon, useIconA, useIconB, useIconC } from './hooks/useAppIcons'

const App = () => {

  // useDefaultIcon();
  // useIconA();
  // useIconB();
  // useIconC();

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