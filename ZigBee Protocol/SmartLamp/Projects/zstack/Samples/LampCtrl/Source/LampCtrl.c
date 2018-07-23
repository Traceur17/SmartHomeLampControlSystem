/******************************************************************************
  Filename:       LampCtrl.c
  Revised:        $Date: 2012-03-07 01:04:58 -0800 (Wed, 07 Mar 2012) $
  Revision:       $Revision: 29656 $

  Description:    Generic Application (no Profile).


  Copyright 2004-2012 Texas Instruments Incorporated. All rights reserved.

  IMPORTANT: Your use of this Software is limited to those specific rights
  granted under the terms of a software license agreement between the user
  who downloaded the software, his/her employer (which must be your employer)
  and Texas Instruments Incorporated (the "License"). You may not use this
  Software unless you agree to abide by the terms of the License. The License
  limits your use, and you acknowledge, that the Software may not be modified,
  copied or distributed unless embedded on a Texas Instruments microcontroller
  or used solely and exclusively in conjunction with a Texas Instruments radio
  frequency transceiver, which is integrated into your product. Other than for
  the foregoing purpose, you may not use, reproduce, copy, prepare derivative
  works of, modify, distribute, perform, display or sell this Software and/or
  its documentation for any purpose.

  YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
  PROVIDED 揂S IS?WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
  INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
  NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
  TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
  NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
  LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
  INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
  OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
  OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
  (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.

  Should you have any questions regarding your right to use this Software,
  contact Texas Instruments Incorporated at www.TI.com.
******************************************************************************/

/*********************************************************************
  This application isn't intended to do anything useful, it is
  intended to be a simple example of an application's structure.

  This application sends "Hello World" to another "Generic"
  application every 5 seconds.  The application will also
  receives "Hello World" packets.

  The "Hello World" messages are sent/received as MSG type message.

  This applications doesn't have a profile, so it handles everything
  directly - itself.

  Key control:
    SW1:
    SW2:  initiates end device binding
    SW3:
    SW4:  initiates a match description request
*********************************************************************/

/*********************************************************************
 * INCLUDES
 */
#include "OSAL.h"
#include "AF.h"
#include "ZDApp.h"
#include "ZDObject.h"
#include "ZDProfile.h"
#include "aps_groups.h"
#include "LampCtrl.h"
#include "DebugTrace.h"


#include "ZGlobals.h"
#include "OnBoard.h"


#include "MT_UART.h"
#include "MT_APP.h"
#include "MT.h"

/* HAL */
#include "hal_lcd.h"
#include "hal_led.h"
#include "hal_key.h"
#include "hal_uart.h"
#include "hal_drivers.h"


uint8 AppTitle[] = "LampCtrl";



#if !defined( LAMPCTRL_PORT )
#define LAMPCTRL_PORT  0
#endif

#if !defined( LAMPCTRL_BAUD )
  #define LAMPCTRL_BAUD  HAL_UART_BR_115200
#endif

// When the Rx buf space is less than this threshold, invoke the Rx callback.
#if !defined( LAMPCTRL_THRESH )
#define LAMPCTRL_THRESH  64
#endif

#if !defined( LAMPCTRL_RX_SZ )
#define LAMPCTRL_RX_SZ  128
#endif

#if !defined( LAMPCTRL_TX_SZ )
#define LAMPCTRL_TX_SZ  128
#endif

// Millisecs of idle time after a byte is received before invoking Rx callback.
#if !defined( LAMPCTRL_IDLE )
#define LAMPCTRL_IDLE  6
#endif

// Loopback Rx bytes to Tx for throughput testing.
#if !defined( LAMPCTRL_LOOPBACK )
#define LAMPCTRL_LOOPBACK  FALSE
#endif

// This is the max byte count per OTA message.
#if !defined( LAMPCTRL_TX_MAX )
#define LAMPCTRL_TX_MAX  80
#endif

#define LAMPCTRL_RSP_CNT  4



// This list should be filled with Application specific Cluster IDs.
const cId_t LampCtrl_ClusterList[LAMPCTRL_MAX_CLUSTERS] =
{
  LAMPCTRL_KEYMSG_CLUSTERID,
  LAMPCTRL_PCMSG_CLUSTERID,
  LAMPCTRL_REGMSG_CLUSTERID
};

const SimpleDescriptionFormat_t LampCtrl_SimpleDesc =
{
  LAMPCTRL_ENDPOINT,              //  int Endpoint;
  LAMPCTRL_PROFID,                //  uint16 AppProfId[2];
  LAMPCTRL_DEVICEID,              //  uint16 AppDeviceId[2];
  LAMPCTRL_DEVICE_VERSION,        //  int   AppDevVer:4;
  LAMPCTRL_FLAGS,                 //  int   AppFlags:4;
  LAMPCTRL_MAX_CLUSTERS,          //  byte  AppNumInClusters;
  (cId_t *)LampCtrl_ClusterList,  //  byte *pAppInClusterList;
  LAMPCTRL_MAX_CLUSTERS,          //  byte  AppNumInClusters;
  (cId_t *)LampCtrl_ClusterList   //  byte *pAppInClusterList;
};



uint8 LampCtrl_TaskID;   // Task ID for internal task/event processing
                          // This variable will be received when
                          // LampCtrl_Init() is called.
devStates_t LampCtrl_NwkState;


uint8 LampCtrl_TransID;  // This is the unique message ID (counter)统计数据发送包 个数

endPointDesc_t LampCtrl_epDesc = 
{
  LAMPCTRL_ENDPOINT,
  &LampCtrl_TaskID,
  (SimpleDescriptionFormat_t *)&LampCtrl_SimpleDesc,
  noLatencyReqs
};

afAddrType_t LampCtrl_Periodic_DstAddr;
afAddrType_t LampCtrl_Flash_DstAddr;

aps_Group_t LampCtrl_Group;


static afAddrType_t LampCtrl_TxAddr;
static uint8 LampCtrl_TxBuf[LAMPCTRL_TX_MAX+1];
static uint8 LampCtrl_TxLen;



/*********************************************************************
 * LOCAL FUNCTIONS
 */
static void LampCtrl_HandleKeys( uint8 shift, uint8 keys );
static void LampCtrl_MessageMSGCB( afIncomingMSGPacket_t *pckt );
static void LampCtrl_CallBack(uint8 port , uint8 event);
static void LampCtrl_Send( void );


/*********************************************************************
 * @fn      LampCtrl_Init
 *
 * @brief   Initialization function for the Generic App Task.
 *          This is called during initialization and should contain
 *          any application specific initialization (ie. hardware
 *          initialization/setup, table initialization, power up
 *          notificaiton ... ).
 *
 * @param   task_id - the ID assigned by OSAL.  This ID should be
 *                    used to send messages and set timers.
 *
 * @return  none
 */
void LampCtrl_Init( uint8 task_id )
{
  LampCtrl_TaskID = task_id;
  LampCtrl_NwkState = DEV_INIT;
  LampCtrl_TransID = 0;
  LampCtrl_TxLen = 0;
  
  
    #if defined ( HOLD_AUTO_START )
  // HOLD_AUTO_START is a compile option that will surpress ZDApp
  //  from starting the device and wait for the application to
  //  start the device.
  ZDOInitDevice(0);
#endif
  
  
  RegisterForKeys( LampCtrl_TaskID );//为按键事件登记在LampCtrl下
  
  halUARTCfg_t uartConfig;//记录串口设置信息

  //初始化串口
  uartConfig.configured           = TRUE;
  uartConfig.baudRate             = LAMPCTRL_BAUD;
  uartConfig.flowControl          = FALSE;
  uartConfig.flowControlThreshold = LAMPCTRL_THRESH;
  uartConfig.rx.maxBufSize        = LAMPCTRL_RX_SZ;
  uartConfig.tx.maxBufSize        = LAMPCTRL_TX_SZ;
  uartConfig.idleTimeout          = LAMPCTRL_IDLE;
  uartConfig.intEnable            = TRUE;         
  uartConfig.callBackFunc         = LampCtrl_CallBack;
  //打开串口
  HalUARTOpen (LAMPCTRL_PORT, &uartConfig);
  
  MT_UartRegisterTaskID(task_id); //注册串口任务


  // Setup for the periodic message's destination address
  // Broadcast to everyone
  LampCtrl_Periodic_DstAddr.addrMode = (afAddrMode_t)AddrBroadcast;
  LampCtrl_Periodic_DstAddr.endPoint = LAMPCTRL_ENDPOINT;
  LampCtrl_Periodic_DstAddr.addr.shortAddr = 0xFFFF;

  // Setup for the flash command's destination address - Group 1
  LampCtrl_Flash_DstAddr.addrMode = (afAddrMode_t)afAddrGroup;
  LampCtrl_Flash_DstAddr.endPoint = LAMPCTRL_ENDPOINT;
  LampCtrl_Flash_DstAddr.addr.shortAddr = LAMPCTRL_FLASH_GROUP;
  
  LampCtrl_TxAddr.addrMode = (afAddrMode_t)Addr16Bit;
  LampCtrl_TxAddr.endPoint = LAMPCTRL_ENDPOINT;
  LampCtrl_TxAddr.addr.shortAddr = 0xFFFF;
    
    
  afRegister( &LampCtrl_epDesc );//将endpoint登记到AF

  // 默认将所有设备分到第一组
  LampCtrl_Group.ID = 0x0001;
  osal_memcpy( LampCtrl_Group.name, "Group 1", 7 );
  aps_AddGroup( LAMPCTRL_ENDPOINT, &LampCtrl_Group );
  
  
  // Update the display
#if defined ( LCD_SUPPORTED )
  HalLcdWriteString( "LampCtrl", HAL_LCD_LINE_1 );
#endif

  ZDO_RegisterForZDOMsg( LampCtrl_TaskID, End_Device_Bind_rsp );
  ZDO_RegisterForZDOMsg( LampCtrl_TaskID, Match_Desc_rsp );

}
/*  end of LampCtrl_Init  */



/*********************************************************************
 * @fn      LampCtrl_ProcessEvent
 *
 * @brief   Generic Application Task event processor.  This function
 *          is called to process all events for the task.  Events
 *          include timers, messages and any other user defined events.
 *
 * @param   task_id  - The OSAL assigned task ID.
 * @param   events - events to process.  This is a bit map and can
 *                   contain more than one event.
 *
 * @return  none
 */
uint16 LampCtrl_ProcessEvent( uint8 task_id, uint16 events )
{
  afIncomingMSGPacket_t *MSGpkt;
  afDataConfirm_t *afDataConfirm;

  // Data Confirmation message fields
  uint8 sentEP;
  ZStatus_t sentStatus;
  uint8 sentTransID;       // This should match the value sent
  (void)task_id;  // Intentionally unreferenced parameter

  if ( events & SYS_EVENT_MSG )
  {
    MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive( LampCtrl_TaskID );
    while ( MSGpkt )
    {
      switch ( MSGpkt->hdr.event )
      {
        case KEY_CHANGE:
          LampCtrl_HandleKeys( ((keyChange_t *)MSGpkt)->state, ((keyChange_t *)MSGpkt)->keys );
          break;
          
        case AF_INCOMING_MSG_CMD:
          LampCtrl_MessageMSGCB( MSGpkt );
          break;


        case AF_DATA_CONFIRM_CMD:
          // This message is received as a confirmation of a data packet sent.
          // The status is of ZStatus_t type [defined in ZComDef.h]
          // The message fields are defined in AF.h
          afDataConfirm = (afDataConfirm_t *)MSGpkt;
          sentEP = afDataConfirm->endpoint;
          sentStatus = afDataConfirm->hdr.status;
          sentTransID = afDataConfirm->transID;
          (void)sentEP;
          (void)sentTransID;

          // Action taken when confirmation is received.
          if ( sentStatus != ZSuccess )
          {
            // The data wasn't delivered -- Do something
          }
          break;

        case ZDO_STATE_CHANGE:
          LampCtrl_NwkState = (devStates_t)(MSGpkt->hdr.status);
          
          if ( (LampCtrl_NwkState == DEV_ROUTER) || (LampCtrl_NwkState == DEV_END_DEVICE) )
          {

            uint8 reg1[17] = "Register,KEY1,on,";
            
            AF_DataRequest(&LampCtrl_Periodic_DstAddr,
                          &LampCtrl_epDesc,
                          LAMPCTRL_REGMSG_CLUSTERID,
                          sizeof(reg1),
                          reg1,
                          &LampCtrl_TransID,
                          AF_DISCV_ROUTE,
                          AF_DEFAULT_RADIUS );
            
            uint8 reg2[17] = "Register,KEY2,on,";
            AF_DataRequest(&LampCtrl_Periodic_DstAddr,
                          &LampCtrl_epDesc,
                          LAMPCTRL_REGMSG_CLUSTERID,
                          sizeof(reg2),
                          reg2,
                          &LampCtrl_TransID,
                          AF_DISCV_ROUTE,
                          AF_DEFAULT_RADIUS );
            
            uint16 flashTime = BUILD_UINT16(LO_UINT16( LAMPCTRL_FLASH_DURATION ), HI_UINT16( LAMPCTRL_FLASH_DURATION ) );
            HalLedBlink( HAL_LED_3, 4, 50, (flashTime / 2) );//3号灯闪烁4次


            
          }
          break;


        default:
          break;
      }

      // Release the memory
      osal_msg_deallocate( (uint8 *)MSGpkt );

      // Next
      MSGpkt = (afIncomingMSGPacket_t *)osal_msg_receive( LampCtrl_TaskID );
    }

    // return unprocessed events
    return (events ^ SYS_EVENT_MSG);
  }

  // Send a message out - This event is generated by a timer
  //  (setup in LampCtrl_Init()).
  if ( events & LAMPCTRL_SEND_MSG_EVT )
  {
    

    // return unprocessed events
    return (events ^ LAMPCTRL_SEND_MSG_EVT);
  }
  
  if ( events & LAMPCTRL_SEND_EVT )
  {
    LampCtrl_Send();
    return ( events^LAMPCTRL_SEND_EVT );
  }

  // Discard unknown events
  return 0;
}


/*********************************************************************
 * @fn      LampCtrl_HandleKeys
 *
 * @brief   Handles all key events for this device.
 *
 * @param   shift - true if in shift/alt.
 * @param   keys - bit field for key events. Valid entries:
 *                 HAL_KEY_SW_4
 *                 HAL_KEY_SW_3
 *                 HAL_KEY_SW_2
 *                 HAL_KEY_SW_1
 *
 * @return  none
 */
static void LampCtrl_HandleKeys( uint8 shift, uint8 keys )
{
  (void) shift;

#if defined ( ZDO_COORDINATOR )

#else
  uint8 ledstate = 0;
  
  uint16 flashTime;
  
  uint8 afstatus;
  
  if ( keys & HAL_KEY_SW_6 )//D1按键
    {
      ledstate = HalLedGetState();
      
        if ( ledstate & 0x01 )//是否是灭
        {
          HalLedSet( HAL_LED_1 , HAL_LED_MODE_OFF );
          uint8 data[16] = "Command,KEY1,on,";
          afstatus = AF_DataRequest(&LampCtrl_Periodic_DstAddr,
                          &LampCtrl_epDesc,
                          LAMPCTRL_KEYMSG_CLUSTERID,
                          sizeof(data),
                          data,
                          &LampCtrl_TransID,
                          AF_DISCV_ROUTE,
                          AF_DEFAULT_RADIUS );
        }
        else
        {
          HalLedSet( HAL_LED_1 , HAL_LED_MODE_ON );
          uint8 data[17] = "Command,KEY1,off,";
          afstatus = AF_DataRequest(&LampCtrl_Periodic_DstAddr,
                          &LampCtrl_epDesc,
                          LAMPCTRL_KEYMSG_CLUSTERID,
                          sizeof(data),
                          data,
                          &LampCtrl_TransID,
                          AF_DISCV_ROUTE,
                          AF_DEFAULT_RADIUS );
        }

      if ( afstatus == afStatus_SUCCESS )
      
      {
        
        
      }
      else
      {
        ledstate = HalLedGetState();
         if ( ledstate & 0x01 )
        {
          HalLedSet( HAL_LED_1 , HAL_LED_MODE_OFF );
        }
        else
        {
          HalLedSet( HAL_LED_1 , HAL_LED_MODE_ON );
        }
        
        flashTime = BUILD_UINT16(LO_UINT16( LAMPCTRL_FLASH_DURATION ), HI_UINT16( LAMPCTRL_FLASH_DURATION ) );
        HalLedBlink( HAL_LED_1, 4, 70, (flashTime / 2) );
        
      }
      
    }
    if ( keys & HAL_KEY_SW_1 )//D2按键
    {
      ledstate = HalLedGetState();
      
      if ( ledstate & 2 )
      {
        HalLedSet(HAL_LED_2 , HAL_LED_MODE_OFF );
        uint8 data[16] = "Command,KEY2,on,";
        afstatus = AF_DataRequest(&LampCtrl_Periodic_DstAddr,
                          &LampCtrl_epDesc,
                          LAMPCTRL_KEYMSG_CLUSTERID,
                          sizeof(data),
                          data,
                          &LampCtrl_TransID,
                          AF_DISCV_ROUTE,
                          AF_DEFAULT_RADIUS );
      }
      else
      {
        HalLedSet(HAL_LED_2 , HAL_LED_MODE_ON );
        uint8 data[17] = "Command,KEY2,off,";
        afstatus = AF_DataRequest(&LampCtrl_Periodic_DstAddr,
                          &LampCtrl_epDesc,
                          LAMPCTRL_KEYMSG_CLUSTERID,
                          sizeof(data),
                          data,
                          &LampCtrl_TransID,
                          AF_DISCV_ROUTE,
                          AF_DEFAULT_RADIUS );
      }      
      if ( afstatus == afStatus_SUCCESS )
      
      {

        
      }
      else
      {
        ledstate = HalLedGetState();
        if ( ledstate & 2 )
        {
          HalLedSet(HAL_LED_2 , HAL_LED_MODE_OFF );
        }
        else
        {
          HalLedSet(HAL_LED_2 , HAL_LED_MODE_ON );
        }
        
        flashTime = BUILD_UINT16(LO_UINT16( LAMPCTRL_FLASH_DURATION ), HI_UINT16( LAMPCTRL_FLASH_DURATION ) );
        HalLedBlink( HAL_LED_2, 4, 70, (flashTime / 2) );
        
      }
    }
#endif
}

/*********************************************************************
 * LOCAL FUNCTIONS
 */

/*********************************************************************
 * @fn      LampCtrl_MessageMSGCB
 *
 * @brief   Data message processor callback.  This function processes
 *          any incoming data - probably from other devices.  So, based
 *          on cluster ID, perform the intended action.
 *
 * @param   none
 *
 * @return  none
 */
static void LampCtrl_MessageMSGCB( afIncomingMSGPacket_t *pkt )
{
  byte data;
  
  switch ( pkt->clusterId )
  {
    case LAMPCTRL_KEYMSG_CLUSTERID:
      HalUARTWrite(0 , pkt->cmd.Data , pkt->cmd.DataLength);
      HalUARTWrite(0 , "\n" , 1);
      break;
      
    case LAMPCTRL_REGMSG_CLUSTERID:
      HalUARTWrite(0 , pkt->cmd.Data , pkt->cmd.DataLength);
      HalUARTWrite(0 , "\n" , 1);
      break;
      
    case LAMPCTRL_PCMSG_CLUSTERID:      
      data = pkt->cmd.Data[0];
      if ( data == 0x10 )//点亮
      {
        HalLedSet( HAL_LED_1 , HAL_LED_MODE_OFF );
      }
      if ( data == 0x11 )//熄灭
      {
        HalLedSet( HAL_LED_1 , HAL_LED_MODE_ON );
      }
      if ( data == 0x20 )
      {
        HalLedSet( HAL_LED_2 , HAL_LED_MODE_OFF );
      }
      if ( data == 0x21 )
      {
        HalLedSet( HAL_LED_2 , HAL_LED_MODE_ON );
      }
      if ( data == 0x00 )
      {
        HalLedSet( HAL_LED_1 , HAL_LED_MODE_OFF );
        HalLedSet( HAL_LED_2 , HAL_LED_MODE_OFF );
      }
    break;
    
  }
}


static void LampCtrl_CallBack(uint8 port , uint8 event)
{
  (void)port;
  if ((event & (HAL_UART_RX_FULL | HAL_UART_RX_ABOUT_FULL | HAL_UART_RX_TIMEOUT)) && !LampCtrl_TxLen)
  {
    LampCtrl_Send();
  }
}

static void LampCtrl_Send( )//从电脑接收数据，准备发给终端
{
    LampCtrl_TxLen = HalUARTRead(LAMPCTRL_PORT, LampCtrl_TxBuf, LAMPCTRL_TX_MAX);
  
    if (LampCtrl_TxLen)
    {
      if (afStatus_SUCCESS != AF_DataRequest(&LampCtrl_TxAddr,
                                             &LampCtrl_epDesc,
                                             LAMPCTRL_PCMSG_CLUSTERID,
                                             LampCtrl_TxLen,
                                             LampCtrl_TxBuf,
                                             &LampCtrl_TransID, 
                                             0, 
                                             AF_DEFAULT_RADIUS))
      {
        osal_set_event(LampCtrl_TaskID, LAMPCTRL_SEND_EVT   /*todo*/  );
      }
      else
      {
        LampCtrl_TxLen = 0;
      }
    }  
}


/*********************************************************************
 */
